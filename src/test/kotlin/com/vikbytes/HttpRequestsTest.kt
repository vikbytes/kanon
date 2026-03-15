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

class HttpRequestsTest {

    @Test
    fun `test createHttpClient with default settings`() {
        val client = HttpRequests.createHttpClient(5000, false)

        assertNotNull(client)
        client.close()
    }

    @Test
    fun `test createHttpClient with follow redirects enabled`() {
        val client = HttpRequests.createHttpClient(10000, true)

        assertNotNull(client)
        client.close()
    }

    @Test
    fun `test createHttpClient with different timeout values`() {
        val shortTimeoutClient = HttpRequests.createHttpClient(100, false)
        assertNotNull(shortTimeoutClient)
        shortTimeoutClient.close()

        val longTimeoutClient = HttpRequests.createHttpClient(60000, false)
        assertNotNull(longTimeoutClient)
        longTimeoutClient.close()
    }

    @Test
    fun `test executeRequest with successful response`() = runBlocking {
        val mockEngine = MockEngine { request ->
            when (request.url.encodedPath) {
                "/test" -> {
                    respond(
                        content = ByteReadChannel("""{"message": "success"}"""),
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
                else -> {
                    respond(content = ByteReadChannel("Not Found"), status = HttpStatusCode.NotFound)
                }
            }
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)
        val histogram = Histogram(3600000L, 2)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/test",
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
            progressTracker = null,
            silent = false,
            noBandwidth = false,
            histogram = histogram,
        )

        assertEquals(1, successCount.get())
        assertEquals(0, failureCount.get())
        assertEquals(1, responseTimes.size)
        assertTrue(responseTimes.peek()!! >= 0)
        assertEquals(1, statusCodes[200]?.get())
        assertTrue(requestBytes.get() > 0)
        assertTrue(responseBytes.get() > 0)
        assertEquals(1, histogram.totalCount)

        client.close()
    }

    @Test
    fun `test executeRequest with failed response`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(content = ByteReadChannel("Internal Server Error"), status = HttpStatusCode.InternalServerError)
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)
        val histogram = Histogram(3600000L, 2)

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
            progressTracker = null,
            silent = false,
            noBandwidth = false,
            histogram = histogram,
        )

        assertEquals(0, successCount.get())
        assertEquals(1, failureCount.get())
        assertEquals(1, responseTimes.size)
        assertEquals(1, statusCodes[500]?.get())
        assertTrue(requestBytes.get() > 0)
        assertTrue(responseBytes.get() > 0)

        client.close()
    }

    @Test
    fun `test executeRequest with headers`() = runBlocking {
        val mockEngine = MockEngine { request ->
            val hasContentType = request.headers.contains(HttpHeaders.ContentType)
            val hasCustomHeader = request.headers.contains("X-Custom-Header")

            respond(content = ByteReadChannel("""{"hasHeaders": true}"""), status = HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/headers",
            method = "GET",
            headers = "Content-Type: application/json\nX-Custom-Header: test-value",
            authorization = null,
            jsonBody = null,
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            progressTracker = null,
            silent = false,
            noBandwidth = false,
            histogram = null,
        )

        assertEquals(1, successCount.get())
        assertEquals(0, failureCount.get())

        client.close()
    }

    @Test
    fun `test executeRequest with authorization`() = runBlocking {
        val mockEngine = MockEngine { request ->
            val hasAuth = request.headers.contains(HttpHeaders.Authorization)

            respond(content = ByteReadChannel("""{"authorized": $hasAuth}"""), status = HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/auth",
            method = "GET",
            headers = null,
            authorization = "Bearer test-token",
            jsonBody = null,
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            progressTracker = null,
            silent = false,
            noBandwidth = false,
            histogram = null,
        )

        assertEquals(1, successCount.get())

        client.close()
    }

    @Test
    fun `test executeRequest with JSON body`() = runBlocking {
        val mockEngine = MockEngine { request ->
            val hasContentType = request.headers[HttpHeaders.ContentType]?.contains("application/json") == true

            respond(content = ByteReadChannel("""{"received": true}"""), status = HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/json",
            method = "POST",
            headers = null,
            authorization = null,
            jsonBody = """{"test": "data"}""",
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            progressTracker = null,
            silent = false,
            noBandwidth = false,
            histogram = null,
        )

        assertEquals(1, successCount.get())
        assertTrue(requestBytes.get() > 50)

        client.close()
    }

    @Test
    fun `test executeRequest with noBandwidth flag`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(content = ByteReadChannel("""{"message": "test"}"""), status = HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/nobandwidth",
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
            progressTracker = null,
            silent = false,
            noBandwidth = true,
            histogram = null,
        )

        assertEquals(1, successCount.get())
        assertEquals(0L, requestBytes.get())
        assertEquals(0L, responseBytes.get())

        client.close()
    }

    @Test
    fun `test executeRequest with silent flag`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(content = ByteReadChannel("""{"message": "test"}"""), status = HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)

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
            progressTracker = null,
            silent = true,
            noBandwidth = false,
            histogram = null,
        )

        assertEquals(0, successCount.get())
        assertEquals(0, failureCount.get())
        assertTrue(responseTimes.isEmpty())
        assertTrue(statusCodes.isEmpty())

        client.close()
    }

    @Test
    fun `test executeRequest with progress tracker`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(content = ByteReadChannel("""{"message": "test"}"""), status = HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)
        val progressTracker = AtomicInteger(0)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/progress",
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
            progressTracker = progressTracker,
            silent = false,
            noBandwidth = false,
            histogram = null,
        )

        assertEquals(1, successCount.get())
        assertEquals(1, progressTracker.get())

        client.close()
    }

    @Test
    fun `test executeRequest with network exception`() = runBlocking {
        val mockEngine = MockEngine { request -> throw RuntimeException("Network error") }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)

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
            progressTracker = null,
            silent = false,
            noBandwidth = false,
            histogram = null,
        )

        assertEquals(0, successCount.get())
        assertEquals(1, failureCount.get())
        assertTrue(responseTimes.isEmpty())
        assertTrue(statusCodes.isEmpty())

        client.close()
    }

    @Test
    fun `test executeRequestsInParallel with number of requests`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(content = ByteReadChannel("""{"message": "test"}"""), status = HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val statistics = StatisticsHelper.RequestStatistics()

        val duration =
            HttpRequests.executeRequestsInParallel(
                client = client,
                url = "http://test.com/parallel",
                method = "GET",
                headers = null,
                authorization = null,
                jsonBody = null,
                concurrency = 2,
                totalRequests = 5,
                durationSeconds = null,
                statistics = statistics,
                progressTracker = null,
                torture = false,
                noBandwidth = false,
            )

        assertTrue(statistics.successCount.get() >= 5)
        assertTrue(duration.inWholeMilliseconds > 0)

        client.close()
    }

    @Test
    fun `test executeRequestsInParallel with duration`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(content = ByteReadChannel("""{"message": "test"}"""), status = HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val statistics = StatisticsHelper.RequestStatistics()

        val duration =
            HttpRequests.executeRequestsInParallel(
                client = client,
                url = "http://test.com/duration",
                method = "GET",
                headers = null,
                authorization = null,
                jsonBody = null,
                concurrency = 2,
                totalRequests = null,
                durationSeconds = 1,
                statistics = statistics,
                progressTracker = null,
                torture = false,
                noBandwidth = false,
            )

        assertTrue(statistics.successCount.get() >= 1)
        assertTrue(duration.inWholeMilliseconds >= 900)

        client.close()
    }

    @Test
    fun `test bandwidth calculation accuracy`() = runBlocking {
        val responseBody = "A".repeat(1000)

        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel(responseBody),
                status = HttpStatusCode.OK,
                headers =
                    headersOf(
                        HttpHeaders.ContentType to listOf("text/plain"),
                        HttpHeaders.ContentLength to listOf(responseBody.length.toString()),
                    ),
            )
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)

        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/bandwidth",
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
            progressTracker = null,
            silent = false,
            noBandwidth = false,
            histogram = null,
        )

        assertEquals(1, successCount.get())
        assertTrue(requestBytes.get() > 20)
        assertTrue(responseBytes.get() > 1000)

        client.close()
    }

    @Test
    fun `test different HTTP methods`() = runBlocking {
        val methods = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")

        for (method in methods) {
            val mockEngine = MockEngine { request ->
                assertEquals(HttpMethod.parse(method), request.method)
                respond(content = ByteReadChannel("""{"method": "$method"}"""), status = HttpStatusCode.OK)
            }

            val client = HttpClient(mockEngine)
            val successCount = AtomicInteger(0)
            val failureCount = AtomicInteger(0)
            val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
            val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
            val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
            val responseBytes = java.util.concurrent.atomic.AtomicLong(0)

            HttpRequests.executeRequest(
                client = client,
                url = "http://test.com/$method",
                method = method,
                headers = null,
                authorization = null,
                jsonBody = null,
                successCount = successCount,
                failureCount = failureCount,
                responseTimes = responseTimes,
                statusCodes = statusCodes,
                requestBytes = requestBytes,
                responseBytes = responseBytes,
                progressTracker = null,
                silent = false,
                noBandwidth = false,
                histogram = null,
            )

            assertEquals(1, successCount.get(), "Failed for method $method")
            client.close()
        }
    }

    @Test
    fun `test responseBytes not double-counted`() = runBlocking {
        val knownBody = "A".repeat(500)
        val mockEngine = MockEngine { request ->
            respond(
                content = ByteReadChannel(knownBody),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "text/plain"),
            )
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)

        repeat(5) {
            HttpRequests.executeRequest(
                client = client,
                url = "http://test.com/double-count",
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
                progressTracker = null,
                silent = false,
                noBandwidth = false,
                histogram = null,
            )
        }

        assertEquals(5, successCount.get())
        // Each response: 500 bytes body + ~170 overhead. If double-counted, would be >5000.
        // With 5 requests, total should be around 5 * 670 = 3350, not 5 * 1170.
        val maxExpectedPerRequest = 500 + 300 // body + generous overhead
        assertTrue(
            responseBytes.get() <= 5L * maxExpectedPerRequest,
            "responseBytes (${responseBytes.get()}) should not exceed ${5L * maxExpectedPerRequest} — body may be double-counted"
        )
        assertTrue(responseBytes.get() >= 5L * 500, "responseBytes should include at least the body size")

        client.close()
    }

    @Test
    fun `test header values containing commas are preserved when using newline separator`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(content = ByteReadChannel("ok"), status = HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)

        // Newline-separated headers — commas within values are preserved
        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/comma-header",
            method = "GET",
            headers = "Accept: text/html, application/json\nX-Custom: value",
            authorization = null,
            jsonBody = null,
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            progressTracker = null,
            silent = false,
            noBandwidth = false,
            histogram = null,
        )

        assertEquals(1, successCount.get())

        // Verify the mock received 2 custom headers (Accept and X-Custom), not 3
        val lastRequest = mockEngine.requestHistory.last()
        val acceptValue = lastRequest.headers["Accept"]
        assertNotNull(acceptValue, "Accept header should be present")
        assertTrue(
            acceptValue.contains("text/html") && acceptValue.contains("application/json"),
            "Accept header should contain full comma-separated value: $acceptValue"
        )

        client.close()
    }

    @Test
    fun `test comma in headers string is not used as separator by executeRequest`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(content = ByteReadChannel("ok"), status = HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)
        val responseTimes = java.util.concurrent.ConcurrentLinkedQueue<Long>()
        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        val requestBytes = java.util.concurrent.atomic.AtomicLong(0)
        val responseBytes = java.util.concurrent.atomic.AtomicLong(0)

        // Pass a single header string containing a comma — should be treated as ONE header
        HttpRequests.executeRequest(
            client = client,
            url = "http://test.com/single-header",
            method = "GET",
            headers = "X-Values: a,b,c",
            authorization = null,
            jsonBody = null,
            successCount = successCount,
            failureCount = failureCount,
            responseTimes = responseTimes,
            statusCodes = statusCodes,
            requestBytes = requestBytes,
            responseBytes = responseBytes,
            progressTracker = null,
            silent = false,
            noBandwidth = false,
            histogram = null,
        )

        assertEquals(1, successCount.get())
        val lastRequest = mockEngine.requestHistory.last()
        val xValues = lastRequest.headers["X-Values"]
        assertEquals("a,b,c", xValues, "Comma in header value should be preserved, not split")

        client.close()
    }

    @Test
    fun `test executeRequestsInParallel returns wall-clock duration`() = runBlocking {
        val mockEngine = MockEngine { request ->
            respond(content = ByteReadChannel("ok"), status = HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val statistics = StatisticsHelper.RequestStatistics()

        val before = System.currentTimeMillis()
        val duration = HttpRequests.executeRequestsInParallel(
            client = client,
            url = "http://test.com/wallclock",
            method = "GET",
            headers = null,
            authorization = null,
            jsonBody = null,
            concurrency = 2,
            totalRequests = 10,
            durationSeconds = null,
            statistics = statistics,
            progressTracker = null,
            torture = false,
            noBandwidth = true,
        )
        val after = System.currentTimeMillis()

        val wallClock = after - before
        // Duration should be close to wall-clock, not the sum of all response times
        assertTrue(
            duration.inWholeMilliseconds <= wallClock + 100,
            "Duration (${duration.inWholeMilliseconds}ms) should not exceed wall-clock (${wallClock}ms) by more than 100ms"
        )
        assertTrue(duration.inWholeMilliseconds >= 0, "Duration should be non-negative")

        client.close()
    }

    @Test
    fun `test exact request count with high concurrency`() = runBlocking {
        var requestCount = java.util.concurrent.atomic.AtomicInteger(0)
        val mockEngine = MockEngine { request ->
            requestCount.incrementAndGet()
            respond(content = ByteReadChannel("ok"), status = HttpStatusCode.OK)
        }

        val client = HttpClient(mockEngine)
        val statistics = StatisticsHelper.RequestStatistics()

        HttpRequests.executeRequestsInParallel(
            client = client,
            url = "http://test.com/exact-count",
            method = "GET",
            headers = null,
            authorization = null,
            jsonBody = null,
            concurrency = 10,
            totalRequests = 10,
            durationSeconds = null,
            statistics = statistics,
            progressTracker = null,
            torture = false,
            noBandwidth = true,
        )

        // requestCount includes the warm-up request (+1)
        // The parallel phase should execute exactly totalRequests (10) requests
        assertEquals(10, statistics.successCount.get(),
            "Should have exactly 10 successful requests (excluding warm-up)")

        client.close()
    }
}
