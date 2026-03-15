package com.vikbytes

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.*
import kotlinx.coroutines.runBlocking
import org.HdrHistogram.Histogram
import org.junit.jupiter.api.Test

class HttpRequestsErrorTest {

    @Test
    fun `createHttpClient should throw exception for zero timeout`() {
        // Zero timeout may not be valid for Ktor HTTP client
        assertFailsWith<IllegalArgumentException> {
            HttpRequests.createHttpClient(0, false)
        }
    }

    @Test
    fun `createHttpClient should throw exception for negative timeout`() {
        // Negative timeout should not be valid for Ktor HTTP client
        assertFailsWith<IllegalArgumentException> {
            HttpRequests.createHttpClient(-1000, false)
        }
    }

    @Test
    fun `createHttpClient should handle very large timeout gracefully`() {
        val client = HttpRequests.createHttpClient(Int.MAX_VALUE, false)

        assertNotNull(client)
        client.close()
    }

    @Test
    fun `executeRequest should handle connection timeout exception`() = runBlocking {
        val mockEngine = MockEngine { request ->
            throw java.net.SocketTimeoutException("Connection timeout")
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)
        val histogram = Histogram(3600000000L, 3)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/timeout",
            method = "GET",
            headers = null,
            authorization = null,
            jsonBody = null,
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            noBandwidth = false,
            histogram = histogram
        )

        assertEquals(0, successCount.get())
        assertEquals(1, failureCount.get())
        assertTrue(responseTimes.isEmpty())
        assertTrue(statusCodes.isEmpty())
        client.close()
    }

    @Test
    fun `executeRequest should handle malformed URL errors`() = runBlocking {
        val mockEngine = MockEngine { request ->
            throw IllegalArgumentException("Invalid URL")
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)
        val histogram = Histogram(3600000000L, 3)

        HttpRequests.executeRequest(
            client = client,
            url = "invalid-url",
            method = "GET",
            headers = null,
            authorization = null,
            jsonBody = null,
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            noBandwidth = false,
            histogram = histogram
        )

        assertEquals(0, successCount.get())
        assertEquals(1, failureCount.get())
        client.close()
    }

    @Test
    fun `executeRequest should handle invalid HTTP method gracefully`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel(""),
                status = HttpStatusCode.MethodNotAllowed,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)
        val histogram = Histogram(3600000000L, 3)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/method",
            method = "INVALID_METHOD",
            headers = null,
            authorization = null,
            jsonBody = null,
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            noBandwidth = false,
            histogram = histogram
        )

        assertEquals(0, successCount.get())
        assertEquals(1, failureCount.get())
        assertEquals(1, statusCodes[405]?.get())
        client.close()
    }

    @Test
    fun `executeRequest should handle malformed headers gracefully`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""{"message": "success"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)
        val histogram = Histogram(3600000000L, 3)

        // Test with malformed headers (missing colon)
        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/test",
            method = "GET",
            headers = "InvalidHeader\nAnotherInvalidHeader",
            authorization = null,
            jsonBody = null,
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            noBandwidth = false,
            histogram = histogram
        )

        // Should still succeed even with malformed headers
        assertEquals(1, successCount.get())
        assertEquals(0, failureCount.get())
        client.close()
    }

    @Test
    fun `executeRequest should handle empty JSON body`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""{"message": "success"}"""),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)
        val histogram = Histogram(3600000000L, 3)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/test",
            method = "POST",
            headers = null,
            authorization = null,
            jsonBody = "", // Empty JSON body
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            noBandwidth = false,
            histogram = histogram
        )

        assertEquals(1, successCount.get())
        assertEquals(0, failureCount.get())
        client.close()
    }

    @Test
    fun `executeRequest should handle server error responses`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""{"error": "Internal server error"}"""),
                status = HttpStatusCode.InternalServerError,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)
        val histogram = Histogram(3600000000L, 3)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/error",
            method = "GET",
            headers = null,
            authorization = null,
            jsonBody = null,
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            noBandwidth = false,
            histogram = histogram
        )

        assertEquals(0, successCount.get())
        assertEquals(1, failureCount.get())
        assertEquals(1, statusCodes[500]?.get())
        assertNotEquals(0, responseTimes.size)
        client.close()
    }

    @Test
    fun `executeRequest should handle client error responses`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel("""{"error": "Not found"}"""),
                status = HttpStatusCode.NotFound,
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)
        val histogram = Histogram(3600000000L, 3)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/notfound",
            method = "GET",
            headers = null,
            authorization = null,
            jsonBody = null,
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            noBandwidth = false,
            histogram = histogram
        )

        assertEquals(0, successCount.get())
        assertEquals(1, failureCount.get())
        assertEquals(1, statusCodes[404]?.get())
        client.close()
    }

    @Test
    fun `executeRequest should handle very large response body`() = runBlocking {
        val largeResponseBody = "x".repeat(1000000) // 1MB response
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel(largeResponseBody),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain")
            )
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)
        val histogram = Histogram(3600000000L, 3)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/large",
            method = "GET",
            headers = null,
            authorization = null,
            jsonBody = null,
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            noBandwidth = false,
            histogram = histogram
        )

        assertEquals(1, successCount.get())
        assertEquals(0, failureCount.get())
        assertEquals(1, statusCodes[200]?.get())
        assertTrue(responseBytes.get() > 1000000) // Should track large response
        client.close()
    }

    @Test
    fun `executeRequest should handle silent mode properly during errors`() = runBlocking {
        val mockEngine = MockEngine { request ->
            throw RuntimeException("Silent error")
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)
        val histogram = Histogram(3600000000L, 3)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/silent",
            method = "GET",
            headers = null,
            authorization = null,
            jsonBody = null,
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            silent = true, // Silent mode should not update counters
            noBandwidth = false,
            histogram = histogram
        )

        // In silent mode, counters should not be updated even on error
        assertEquals(0, successCount.get())
        assertEquals(0, failureCount.get())
        assertTrue(responseTimes.isEmpty())
        assertTrue(statusCodes.isEmpty())
        client.close()
    }
}