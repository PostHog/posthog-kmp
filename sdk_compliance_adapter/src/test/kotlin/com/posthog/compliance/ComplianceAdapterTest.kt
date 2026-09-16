package com.posthog.compliance

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.sun.net.httpserver.HttpServer
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.URI
import java.util.Collections
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

class ComplianceAdapterTest {
    @Test
    fun timestampUsesExistingEpochMillisInput() {
        val input = JsonParser.parseString("""{"timestamp":"2025-01-02T08:34:05+05:30"}""").asJsonObject
        assertEquals(1735787045000L, ComplianceAdapter.timestamp(input))
        assertNull(ComplianceAdapter.timestamp(JsonObject()))
    }

    @Test
    fun wireObserverCountsRetriesWithoutInventingQueueState() {
        WireObserver(19322).use { observer ->
            val body = """{"batch":[{"uuid":"sdk-generated-id"}]}""".toByteArray()
            val gzip = ByteArrayOutputStream().also { output -> GZIPOutputStream(output).use { it.write(body) } }.toByteArray()
            observer.beforeSend()
            observer.record(gzip, true, 503, 1000)
            assertEquals(0, observer.sentCount())
            observer.record(gzip, true, 200, 2000)
            assertNull(observer.state()["total_events_captured"])
            assertEquals(1, observer.state()["total_retries"])
            assertEquals(1, observer.sentCount())
            assertNull(observer.state()["pending_events"])
            observer.reset()
            assertEquals(0, observer.sentCount())
        }
    }

    @Test(expected = IllegalArgumentException::class)
    fun observationProxyRejectsNonLocalHosts() {
        WireObserver(19322).use { it.configure("https://us.i.posthog.com") }
    }

    @Test
    fun proxyPreservesWireBytesAndRetryAfterWithoutRetrying() {
        val body = """{"batch":[{"uuid":"sdk-id"}]}""".toByteArray()
        val bytes = ByteArrayOutputStream().also { output -> GZIPOutputStream(output).use { it.write(body) } }.toByteArray()
        val received = Collections.synchronizedList(mutableListOf<ByteArray>())
        val mock = HttpServer.create(InetSocketAddress("127.0.0.1", 19323), 0)
        mock.createContext("/batch") { exchange ->
            received.add(exchange.requestBody.readBytes())
            exchange.responseHeaders.set("Retry-After", "3")
            exchange.sendResponseHeaders(503, 5)
            exchange.responseBody.use { it.write("retry".toByteArray()) }
            exchange.close()
        }
        mock.start()
        var connection: HttpURLConnection? = null
        try {
            WireObserver(19322).use { observer ->
                observer.configure("http://127.0.0.1:19323")
                val request = URI("${observer.host}/batch").toURL().openConnection() as HttpURLConnection
                connection = request
                request.requestMethod = "POST"
                request.doOutput = true
                request.setRequestProperty("Content-Encoding", "gzip")
                request.outputStream.use { it.write(bytes) }
                assertEquals(503, request.responseCode)
                assertEquals("3", request.getHeaderField("Retry-After"))
                assertEquals("retry", request.errorStream.bufferedReader().use { it.readText() })
                assertEquals(1, received.size)
                assertArrayEquals(bytes, received.single())
                assertEquals(0, observer.sentCount())
            }
        } finally {
            connection?.disconnect()
            mock.stop(0)
        }
    }

    @Test
    fun publicFacadePreservesBatchUUIDsAndTimestampsOnRetry() {
        val batches = Collections.synchronizedList(mutableListOf<JsonObject>())
        val mock = HttpServer.create(InetSocketAddress("127.0.0.1", 19323), 0)
        mock.createContext("/") { exchange ->
            val raw = exchange.requestBody.readBytes()
            var status = 200
            if (exchange.requestURI.path.trimEnd('/') == "/batch") {
                val json = GZIPInputStream(raw.inputStream()).use { it.readBytes() }.toString(Charsets.UTF_8)
                batches.add(JsonParser.parseString(json).asJsonObject)
                if (batches.size == 1) status = 503
            }
            exchange.sendResponseHeaders(status, 2)
            exchange.responseBody.use { it.write("{}".toByteArray()) }
            exchange.close()
        }
        mock.start()
        try {
            WireObserver(19322).use { observer ->
                ComplianceAdapter(observer).use { adapter ->
                    adapter.handle("/init", """{
                        "api_key":"phc_retry_test", "host":"http://127.0.0.1:19323",
                        "flush_at":100, "flush_interval_ms":1000
                    }""")
                    repeat(3) { index ->
                        adapter.handle("/capture", """{
                            "distinct_id":"retry-user", "event":"retry-event-$index",
                            "timestamp":"2025-01-02T08:34:05+05:30"
                        }""")
                    }
                    adapter.handle("/flush", "")
                    val attempts = synchronized(batches) { batches.map { it.getAsJsonArray("batch").toList() } }
                    assertEquals(2, attempts.size)
                    assertEquals(4, attempts.first().size)
                    assertEquals(attempts[0].map { it.asJsonObject.get("uuid") }, attempts[1].map { it.asJsonObject.get("uuid") })
                    assertEquals(attempts[0].map { it.asJsonObject.get("timestamp") }, attempts[1].map { it.asJsonObject.get("timestamp") })
                    assertEquals(1, observer.state()["total_retries"])
                    assertEquals(4, observer.sentCount())
                }
            }
        } finally {
            mock.stop(0)
        }
    }

    @Test
    fun publicFacadePreservesIdentitySideEffectsAndCaptureTimestamp() {
        val batches = Collections.synchronizedList(mutableListOf<JsonObject>())
        val mock = HttpServer.create(InetSocketAddress("127.0.0.1", 19323), 0)
        mock.createContext("/") { exchange ->
            val raw = exchange.requestBody.readBytes()
            if (exchange.requestURI.path.trimEnd('/') == "/batch") {
                assertEquals("gzip", exchange.requestHeaders.getFirst("Content-Encoding"))
                val json = GZIPInputStream(raw.inputStream()).use { it.readBytes() }.toString(Charsets.UTF_8)
                batches.add(JsonParser.parseString(json).asJsonObject)
            }
            val response = """{"featureFlags":{"test-flag":"variant-a"}}""".toByteArray()
            exchange.responseHeaders.set("Content-Type", "application/json")
            exchange.sendResponseHeaders(200, response.size.toLong())
            exchange.responseBody.use { it.write(response) }
            exchange.close()
        }
        mock.start()
        try {
            WireObserver(19322).use { observer ->
                ComplianceAdapter(observer).use { adapter ->
                    val init = JsonParser.parseString(adapter.handle("/init", """{
                        "api_key":"phc_local_test", "host":"http://127.0.0.1:19323",
                        "flush_at":100, "flush_interval_ms":500, "max_retries":3
                    }""")).asJsonObject
                    assertTrue(init.getAsJsonArray("unsupported_controls").toString().contains("flush_interval_ms"))
                    val capture = JsonParser.parseString(adapter.handle("/capture", """{
                        "distinct_id":"test-user", "event":"timestamp-event",
                        "timestamp":"2025-01-02T08:34:05+05:30",
                        "properties":{
                            "timestamp_like":"2025-01-02T08:34:05+05:30", "custom":42,
                            "large_integer":9007199254740993,
                            "nested":{"large_integer":9007199254740993}, "omit_me":null
                        }
                    }""")).asJsonObject
                    assertFalse(capture.has("uuid"))
                    val flush = JsonParser.parseString(adapter.handle("/flush", "")).asJsonObject
                    assertFalse(flush.get("success").asBoolean)
                    assertTrue(flush.get("flush_invoked").asBoolean)
                    val events = synchronized(batches) { batches.flatMap { it.getAsJsonArray("batch").map { event -> event.asJsonObject } } }
                    assertTrue(events.any { it.get("event").asString == "\$identify" })
                    val event = events.single { it.get("event").asString == "timestamp-event" }
                    assertEquals("test-user", event.get("distinct_id").asString)
                    assertEquals("2025-01-02T03:04:05.000Z", event.get("timestamp").asString)
                    assertEquals("posthog-kmp", event.getAsJsonObject("properties").get("\$lib").asString)
                    assertEquals("2025-01-02T08:34:05+05:30", event.getAsJsonObject("properties").get("timestamp_like").asString)
                    val properties = event.getAsJsonObject("properties")
                    assertTrue(properties.getAsJsonPrimitive("large_integer").isNumber)
                    assertEquals(9007199254740993L, properties.get("large_integer").asLong)
                    assertEquals(9007199254740993L, properties.getAsJsonObject("nested").get("large_integer").asLong)
                    assertFalse(properties.has("omit_me"))
                    assertTrue(event.get("uuid").asString.isNotBlank())
                    val flag = JsonParser.parseString(adapter.handle("/get_feature_flag", """{
                        "distinct_id":"test-user", "key":"test-flag", "force_remote":true
                    }""")).asJsonObject
                    assertEquals("variant-a", flag.get("value").asString)
                    adapter.handle("/flush", "")
                    val called = synchronized(batches) { batches.flatMap { it.getAsJsonArray("batch").toList() } }
                    assertTrue(called.any { it.asJsonObject.get("event").asString == "\$feature_flag_called" })
                    adapter.handle("/reset", "")
                    assertEquals(0, observer.sentCount())
                }
            }
        } finally {
            mock.stop(0)
        }
    }
}
