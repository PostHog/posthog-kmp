package com.posthog.compliance

import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.URI
import java.util.concurrent.Executors
import java.util.zip.GZIPInputStream

/** Observes copies of wire bytes; the SDK still owns serialization, compression and retries. */
internal class WireObserver(port: Int) : AutoCloseable {
    private val executor = Executors.newCachedThreadPool()
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
    private val requests = mutableListOf<Map<String, Any>>()
    private val attempts = mutableMapOf<String, Int>()
    private val sent = mutableSetOf<String>()
    private var beforeSendInvocations = 0
    private var lastError: String? = null

    @Volatile
    private var upstream: URI? = null

    val host: String = "http://127.0.0.1:${server.address.port}"

    init {
        server.executor = executor
        server.createContext("/") { exchange -> forward(exchange) }
        server.start()
    }

    fun configure(host: String) {
        val uri = URI(host)
        require(uri.scheme == "http" && uri.host != null && uri.userInfo == null) {
            "Only local HTTP mock hosts are supported"
        }
        require(InetAddress.getAllByName(uri.host).all { it.isLoopbackAddress }) {
            "Only loopback mock traffic is allowed"
        }
        require(uri.port != server.address.port) { "Mock host cannot be the observation proxy" }
        upstream = uri
    }

    @Synchronized
    fun beforeSend() {
        beforeSendInvocations++
    }

    @Synchronized
    fun reset() {
        requests.clear()
        attempts.clear()
        sent.clear()
        beforeSendInvocations = 0
        lastError = null
    }

    @Synchronized
    fun sentCount(): Int = sent.size

    @Synchronized
    fun state(): Map<String, Any?> = mapOf(
        "pending_events" to null,
        "total_events_captured" to null,
        "before_send_invocations" to beforeSendInvocations,
        "total_events_sent" to sent.size,
        "total_retries" to requests.sumOf { if ((it["retry_attempt"] as Int) > 0) 1 else 0 },
        "last_error" to lastError,
        "requests_made" to requests.toList(),
        "requests_scope" to "capture /batch only",
        "observation" to "Wire responses and hook invocations; SDK queue size and captured-event count are unavailable",
    )

    @Synchronized
    internal fun record(body: ByteArray, gzip: Boolean, status: Int, timestamp: Long) {
        val decoded = if (gzip) GZIPInputStream(body.inputStream()).use { it.readBytes() } else body
        val events = JsonParser.parseString(decoded.toString(Charsets.UTF_8)).asJsonObject
            .getAsJsonArray("batch") ?: return
        val ids = events.map { it.asJsonObject.get("uuid").asString }
        val retry = ids.maxOfOrNull { attempts[it] ?: 0 } ?: 0
        ids.forEach { attempts[it] = (attempts[it] ?: 0) + 1 }
        if (status in 200..299) sent.addAll(ids)
        if (status !in 200..299) lastError = "Capture HTTP status $status"
        requests.add(mapOf(
            "timestamp_ms" to timestamp,
            "status_code" to status,
            "retry_attempt" to retry,
            "event_count" to events.size(),
            "uuid_list" to ids,
        ))
    }

    private fun forward(exchange: HttpExchange) {
        var connection: HttpURLConnection? = null
        try {
            val target = checkNotNull(upstream) { "SDK not initialized" }
            val url = URI(target.toString().trimEnd('/') + exchange.requestURI.toString()).toURL()
            connection = url.openConnection() as HttpURLConnection
            connection.instanceFollowRedirects = false
            connection.connectTimeout = 5000
            connection.readTimeout = 10000
            connection.requestMethod = exchange.requestMethod
            exchange.requestHeaders.filterKeys { it.lowercase() !in HOP_HEADERS }.forEach { (key, values) ->
                values.forEach { connection.addRequestProperty(key, it) }
            }
            val body = exchange.requestBody.use { it.readBytes() }
            val timestamp = System.currentTimeMillis()
            if (body.isNotEmpty()) {
                connection.doOutput = true
                connection.setFixedLengthStreamingMode(body.size)
                connection.outputStream.use { it.write(body) }
            }
            val status = connection.responseCode
            val response = (if (status >= 400) connection.errorStream else connection.inputStream)
                ?.use { it.readBytes() } ?: byteArrayOf()
            if (exchange.requestURI.path.trimEnd('/') == "/batch") {
                try {
                    record(body, exchange.requestHeaders.getFirst("Content-Encoding") == "gzip", status, timestamp)
                } catch (error: Exception) {
                    synchronized(this) { lastError = "Wire observation failed: ${error.message}" }
                }
            }
            connection.headerFields.forEach { (key, values) ->
                if (key != null && key.lowercase() !in HOP_HEADERS) {
                    exchange.responseHeaders[key] = values
                }
            }
            exchange.sendResponseHeaders(status, if (response.isEmpty()) -1 else response.size.toLong())
            exchange.responseBody.use { it.write(response) }
        } catch (error: Exception) {
            synchronized(this) { lastError = "Proxy transport failed: ${error.message}" }
            // Closing the connection preserves failure rather than inventing an HTTP retry response.
        } finally {
            connection?.disconnect()
            exchange.close()
        }
    }

    override fun close() {
        server.stop(0)
        executor.shutdownNow()
    }

    private companion object {
        val HOP_HEADERS = setOf("host", "connection", "content-length", "transfer-encoding", "keep-alive")
    }
}
