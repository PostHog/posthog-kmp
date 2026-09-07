package com.posthog.compliance

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.posthog.kmp.CaptureOptions
import com.posthog.kmp.PostHog
import com.posthog.kmp.PostHogBeforeSend
import com.posthog.kmp.PostHogConfig
import com.posthog.kmp.PostHogContext
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.file.Files
import java.time.OffsetDateTime
import java.util.Properties
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

internal class ComplianceAdapter(private val observer: WireObserver) : AutoCloseable {
    private val gson = GsonBuilder().serializeNulls().create()
    private var initialized = false
    private val home = Files.createTempDirectory("posthog-kmp-compliance-").toFile()
    private val previousHome = System.getProperty("user.home")

    init {
        // This dedicated JVM owns its temporary SDK persistence. Never touch the developer's SDK data.
        System.setProperty("user.home", home.absolutePath)
    }

    fun handle(path: String, body: String): String {
        val input = if (body.isBlank()) JsonObject() else JsonParser.parseString(body).asJsonObject
        val response = when (path) {
            "/health" -> mapOf(
                "sdk_name" to "posthog-kmp-jvm",
                "sdk_version" to sdkVersion(),
                "adapter_version" to "1.0.0",
                "supports_parallel" to false,
                "capabilities" to listOf("capture_v0", "encoding_gzip"),
                "runtime" to "KMP JVM facade / stateful com.posthog:posthog",
                "unavailable" to listOf(
                    "capture_uuid", "pending_events", "total_events_captured", "flush_completion",
                    "max_retries", "enable_compression",
                ),
            )
            "/init" -> initialize(input)
            "/reset" -> {
                reset()
                mapOf("success" to true)
            }
            "/state" -> observer.state()
            else -> {
                check(initialized) { "Initialize the SDK first" }
                when (path) {
                    "/capture" -> capture(input)
                    "/get_feature_flag" -> featureFlag(input)
                    "/flush" -> flush()
                    else -> error("Unknown endpoint: $path")
                }
            }
        }
        return gson.toJson(response)
    }

    private fun initialize(input: JsonObject): Map<String, Any> {
        reset()
        observer.configure(input.get("host").asString)
        val defaults = PostHogConfig(input.get("api_key").asString)
        val unsupported = mutableListOf<String>()
        listOf("max_retries", "enable_compression", "disable_geoip", "historical_migration").forEach {
            if (input.has(it)) unsupported.add(it)
        }
        val interval = input.get("flush_interval_ms")?.asInt
        val seconds = if (interval != null && interval > 0 && interval % 1000 == 0) {
            interval / 1000
        } else {
            if (interval != null) unsupported.add("flush_interval_ms: requires positive whole seconds")
            defaults.flushIntervalSeconds
        }
        PostHog.setup(
            defaults.copy(
                host = observer.host,
                flushAt = input.get("flush_at")?.asInt ?: defaults.flushAt,
                flushIntervalSeconds = seconds,
                captureApplicationLifecycleEvents = false,
                captureScreenViews = false,
                captureDeepLinks = false,
                autocapture = false,
                preloadFeatureFlags = false,
                beforeSend = listOf(PostHogBeforeSend { event ->
                    observer.beforeSend()
                    event
                }),
            ),
            PostHogContext(),
        )
        initialized = true
        return mapOf("success" to true, "unsupported_controls" to unsupported)
    }

    private fun capture(input: JsonObject): Map<String, Any> {
        identify(input.get("distinct_id").asString)
        PostHog.capture(
            input.get("event").asString,
            properties(input, "properties"),
            CaptureOptions(timestamp = timestamp(input)),
        )
        return mapOf("success" to true, "observation" to "Capture UUID is not exposed by the KMP facade")
    }

    private fun identify(distinctId: String) {
        if (PostHog.getDistinctId() != distinctId) PostHog.identify(distinctId)
    }

    private fun featureFlag(input: JsonObject): Map<String, Any?> {
        identify(input.get("distinct_id").asString)
        properties(input, "person_properties")?.let { PostHog.setPersonProperties(it) }
        val groupProperties = input.getAsJsonObject("group_properties")
        input.getAsJsonObject("groups")?.entrySet()?.forEach { (type, key) ->
            PostHog.group(type, key.asString, groupProperties?.let { properties(it, type) })
        }
        val loaded = CountDownLatch(1)
        PostHog.reloadFeatureFlags { loaded.countDown() }
        check(loaded.await(15, TimeUnit.SECONDS)) { "Public reloadFeatureFlags callback timed out" }
        return mapOf(
            "success" to true,
            "value" to PostHog.getFeatureFlag(input.get("key").asString),
            "profile" to "public identify/setters, reload, cached getter; SDK side effects retained",
            "unsupported_controls" to if (input.has("disable_geoip")) listOf("disable_geoip") else emptyList<String>(),
        )
    }

    private fun flush(): Map<String, Any?> {
        val before = observer.sentCount()
        PostHog.flush()
        // KMP exposes neither a completion callback nor the queue. Observing traffic cannot prove
        // quiescence or permanent drops; even beforeSend can run more than once for the same event.
        Thread.sleep(TimeUnit.SECONDS.toMillis(10))
        return mapOf(
            "success" to false,
            "flush_invoked" to true,
            "events_flushed" to observer.sentCount() - before,
            "pending_events" to null,
            "observation" to "Observed wire successes for 10 seconds; flush completion and SDK queue/drop state are unavailable",
        )
    }

    private fun properties(input: JsonObject, key: String): Map<String, Any>? {
        val value = input.get(key)?.takeUnless { it.isJsonNull } ?: return null
        val properties: Map<String, Any?> = gson.fromJson(value, object : TypeToken<Map<String, Any?>>() {}.type)
        return properties.mapNotNull { (name, property) -> property?.let { name to it } }.toMap()
    }

    private fun reset() {
        if (initialized) PostHog.close()
        initialized = false
        home.resolve(".posthog-kmp").deleteRecursively()
        observer.reset()
    }

    override fun close() {
        reset()
        home.deleteRecursively()
        if (previousHome == null) System.clearProperty("user.home") else System.setProperty("user.home", previousHome)
    }

    private fun sdkVersion(): String {
        val version = Properties()
        javaClass.getResourceAsStream("/version.properties")!!.use { version.load(it) }
        return listOf("VERSION_MAJOR", "VERSION_MINOR", "VERSION_PATCH").joinToString(".") { version.getProperty(it) }
    }

    internal companion object {
        fun timestamp(input: JsonObject): Long? = input.get("timestamp")?.takeUnless { it.isJsonNull }
            ?.asString?.let { OffsetDateTime.parse(it).toInstant().toEpochMilli() }
    }
}

fun main() {
    val port = System.getenv("PORT")?.toInt() ?: 18320
    val proxyPort = System.getenv("PROXY_PORT")?.toInt() ?: 19321
    val observer = WireObserver(proxyPort)
    val adapter = ComplianceAdapter(observer)
    val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
    server.createContext("/") { exchange ->
        try {
            val response = adapter.handle(exchange.requestURI.path, exchange.requestBody.bufferedReader().use { it.readText() })
                .toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        } catch (error: Exception) {
            val response = GsonBuilder().create().toJson(mapOf("success" to false, "error" to error.message)).toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(400, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        } finally {
            exchange.close()
        }
    }
    Runtime.getRuntime().addShutdownHook(Thread {
        server.stop(0)
        adapter.close()
        observer.close()
    })
    server.start()
    println("KMP JVM facade adapter listening on $port (wire observer $proxyPort)")
}
