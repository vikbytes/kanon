package com.vikbytes

import com.github.ajalt.clikt.core.UsageError
import kotlin.test.*

class CurlHelpersTest {

    @Test
    fun `test splitRespectingQuotes with simple string`() {
        val input = "one two three"
        val result = CurlHelpers.splitRespectingQuotes(input)
        assertEquals(listOf("one", "two", "three"), result)
    }

    @Test
    fun `test splitRespectingQuotes with double quotes`() {
        val input = "one \"two three\" four"
        val result = CurlHelpers.splitRespectingQuotes(input)
        assertEquals(listOf("one", "\"two three\"", "four"), result)
    }

    @Test
    fun `test splitRespectingQuotes with single quotes`() {
        val input = "one 'two three' four"
        val result = CurlHelpers.splitRespectingQuotes(input)
        assertEquals(listOf("one", "'two three'", "four"), result)
    }

    @Test
    fun `test parseCurlCommand with simple URL`() {
        val curlString = "curl https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("GET", result.method)
        assertTrue(result.headers.isEmpty())
        assertEquals(null, result.body)
    }

    @Test
    fun `test parseCurlCommand with method`() {
        val curlString = "curl -X POST https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("POST", result.method)
        assertTrue(result.headers.isEmpty())
        assertEquals(null, result.body)
    }

    @Test
    fun `test parseCurlCommand with headers`() {
        val curlString =
            "curl -H \"Content-Type: application/json\" -H \"Authorization: Bearer token\" https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("GET", result.method)
        assertEquals(2, result.headers.size)
        assertEquals("application/json", result.headers["Content-Type"])
        assertEquals("Bearer token", result.headers["Authorization"])
        assertEquals(null, result.body)
    }

    @Test
    fun `test parseCurlCommand with body`() {
        val curlString = "curl -d '{\"key\":\"value\"}' https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("POST", result.method) // Should be changed to POST when body is present
        assertTrue(result.headers.isEmpty())
        assertEquals("{\"key\":\"value\"}", result.body)
    }

    @Test
    fun `test parseCurlCommand with all parameters`() {
        val curlString =
            "curl -X PUT -H \"Content-Type: application/json\" -d '{\"key\":\"value\"}' https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("PUT", result.method)
        assertEquals(1, result.headers.size)
        assertEquals("application/json", result.headers["Content-Type"])
        assertEquals("{\"key\":\"value\"}", result.body)
    }

    @Test
    fun `test parseCurlCommand without URL throws exception`() {
        val curlString = "curl -X GET"
        assertFailsWith<IllegalArgumentException> { CurlHelpers.parseCurlCommand(curlString) }
    }

    @Test
    fun `test processCurlCommand with simple URL`() {
        val curlString = "curl https://example.com"
        val result = CurlHelpers.processCurlCommand(curlString, false, "")

        assertEquals("https://example.com", result["url"])
        assertEquals("GET", result["method"])
        assertEquals(null, result["headers"])
        assertEquals(null, result["body"])
    }

    @Test
    fun `test processCurlCommand with headers and authorization`() {
        val curlString =
            "curl -H \"Content-Type: application/json\" -H \"Authorization: Bearer token\" https://example.com"
        val result = CurlHelpers.processCurlCommand(curlString, false, "")

        assertEquals("https://example.com", result["url"])
        assertEquals("GET", result["method"])
        assertEquals("Content-Type: application/json,Authorization: Bearer token", result["headers"])
        assertEquals("Bearer token", result["authorization"])
        assertEquals(null, result["body"])
        assertEquals("false", result["followRedirects"])
    }

    @Test
    fun `test processCurlCommand with follow redirects option`() {
        val curlString = "curl -L https://example.com"
        val result = CurlHelpers.processCurlCommand(curlString, false, "")

        assertEquals("https://example.com", result["url"])
        assertEquals("GET", result["method"])
        assertEquals("true", result["followRedirects"])
    }

    @Test
    fun `test processCurlCommand with invalid curl command throws UsageError`() {
        val curlString = "curl -X GET"
        assertFailsWith<UsageError> { CurlHelpers.processCurlCommand(curlString, false, "") }
    }

    @Test
    fun `test parseCurlCommand with URL containing query parameters`() {
        val curlString = "curl https://example.com/api/search?query=test&page=1&limit=10"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com/api/search?query=test&page=1&limit=10", result.url)
        assertEquals("GET", result.method)
        assertTrue(result.headers.isEmpty())
        assertEquals(null, result.body)
    }

    @Test
    fun `test parseCurlCommand with URL containing fragment`() {
        val curlString = "curl https://example.com/page#section1"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com/page#section1", result.url)
        assertEquals("GET", result.method)
    }

    @Test
    fun `test parseCurlCommand with URL containing special characters`() {
        val curlString = "curl \"https://example.com/path with spaces/search?q=special%20chars&filter=test+value\""
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com/path with spaces/search?q=special%20chars&filter=test+value", result.url)
        assertEquals("GET", result.method)
    }

    @Test
    fun `test parseCurlCommand with headers containing special characters`() {
        val curlString = "curl -H \"X-Custom-Header: Value with spaces and symbols: !@#$%^&*()\" https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("Value with spaces and symbols: !@#$%^&*()", result.headers["X-Custom-Header"])
    }

    @Test
    fun `test parseCurlCommand with multiple complex headers`() {
        val curlString =
            "curl -H \"Content-Type: application/json; charset=utf-8\" " +
                "-H \"Accept-Language: en-US,en;q=0.9,es;q=0.8\" " +
                "-H \"User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64)\" " +
                "https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("application/json; charset=utf-8", result.headers["Content-Type"])
        assertEquals("en-US,en;q=0.9,es;q=0.8", result.headers["Accept-Language"])
        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64)", result.headers["User-Agent"])
    }

    @Test
    fun `test parseCurlCommand with JSON body containing nested structures`() {
        val curlString =
            "curl -X POST -H \"Content-Type: application/json\" " +
                "-d '{\"user\":{\"name\":\"John Doe\",\"age\":30,\"address\":{\"street\":\"123 Main St\",\"city\":\"Anytown\"}},\"items\":[1,2,3]}' " +
                "https://example.com/api"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("POST", result.method)
        assertContains(result.body ?: "", "\"user\":{\"name\":\"John Doe\"")
        assertContains(result.body ?: "", "\"items\":[1,2,3]")
    }

    @Test
    fun `test parseCurlCommand with form data`() {
        val curlString =
            "curl -X POST -H \"Content-Type: application/x-www-form-urlencoded\" " +
                "-d \"name=John+Doe&age=30&email=john%40example.com\" " +
                "https://example.com/submit"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("POST", result.method)
        assertEquals("name=John+Doe&age=30&email=john%40example.com", result.body)
    }

    @Test
    fun `test parseCurlCommand with no curl prefix`() {
        val curlString = "https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("GET", result.method)
    }

    @Test
    fun `test parseCurlCommand with unusual HTTP methods`() {
        val methods = listOf("PATCH", "OPTIONS", "HEAD", "TRACE", "CONNECT")
        for (method in methods) {
            val curlString = "curl -X $method https://example.com"
            val result = CurlHelpers.parseCurlCommand(curlString)
            assertEquals(method, result.method)
            assertEquals("https://example.com", result.url)
        }
    }

    @Test
    fun `test parseCurlCommand with inline data parameter`() {
        val curlString = "curl -d@/path/to/file.json https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)
        assertEquals("POST", result.method)
        assertEquals("@/path/to/file.json", result.body)
    }

    @Test
    fun `test parseCurlCommand with quoted text in headers`() {
        val curlString = "curl -H 'X-Custom-Header: Value with \"quoted\" text' https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)
        assertEquals("Value with \"quoted\" text", result.headers["X-Custom-Header"])
    }

    @Test
    fun `test parseCurlCommand with multiple combined flags`() {
        val curlString =
            "curl -sS -X POST -L --compressed -H 'Accept: application/json' " +
                "-H 'Content-Type: application/json' -d '{\"query\":\"mutation\"}' " +
                "https://api.example.com/graphql"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://api.example.com/graphql", result.url)
        assertEquals("POST", result.method)
        assertEquals("application/json", result.headers["Accept"])
        assertEquals("application/json", result.headers["Content-Type"])
        assertEquals("{\"query\":\"mutation\"}", result.body)
    }

    @Test
    fun `test parseCurlCommand with complex URL structure`() {
        val curlString =
            "curl 'https://example.com/api/v2/search?q=test%20query&category=software" +
                "&filter[status]=active&sort=relevance&page=1&per_page=20#results'" +
                " -H 'User-Agent: Mozilla/5.0'"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals(
            "https://example.com/api/v2/search?q=test%20query&category=software" +
                "&filter[status]=active&sort=relevance&page=1&per_page=20#results",
            result.url)
        assertEquals("GET", result.method)
        assertEquals("Mozilla/5.0", result.headers["User-Agent"])
    }

    @Test
    fun `test parseCurlCommand with multipart form data`() {
        val curlString =
            "curl -X POST https://example.com/upload " +
                "-F 'name=John Doe' " +
                "-F 'email=john@example.com' " +
                "-F 'file=@/path/to/file.pdf'"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com/upload", result.url)
        assertEquals("POST", result.method)
        // Note: The current implementation doesn't specifically handle multipart form data,
        // so we're not testing the body content here
    }

    @Test
    fun `test parseCurlCommand with very long complex command`() {
        val curlString =
            "curl 'https://api.example.com/v2/endpoint' " +
                "-H 'Accept: application/json, text/plain, */*' " +
                "-H 'Accept-Language: en-US,en;q=0.9,es;q=0.8,de;q=0.7' " +
                "-H 'Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ' " +
                "-H 'Cache-Control: no-cache' " +
                "-H 'Connection: keep-alive' " +
                "-H 'Content-Type: application/json' " +
                "-H 'Cookie: session=abc123; user_id=12345; theme=dark' " +
                "-H 'Pragma: no-cache' " +
                "-H 'Referer: https://example.com/page' " +
                "-H 'Sec-Fetch-Dest: empty' " +
                "-H 'User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)' " +
                "--data '{\"query\":{\"bool\":{\"must\":[{\"term\":{\"status\":\"active\"}},{\"range\":{\"date\":{\"gte\":\"2023-01-01\"}}}],\"should\":[{\"match\":{\"description\":\"important\"}},{\"match\":{\"title\":\"urgent\"}}],\"minimum_should_match\":1,\"filter\":{\"term\":{\"category\":\"critical\"}}}},\"sort\":[{\"date\":{\"order\":\"desc\"}}],\"size\":100}' " +
                "--compressed"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://api.example.com/v2/endpoint", result.url)
        assertEquals(11, result.headers.size)
        assertContains(result.body ?: "", "\"query\":{\"bool\":{\"must\":[{\"term\":{\"status\":\"active\"}}")
        assertContains(result.headers["Authorization"] ?: "", "Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9")
    }

    @Test
    fun `test parseCurlCommand with data-urlencode parameter`() {
        val curlString = "curl --data-urlencode \"name=John Doe\" https://example.com/api"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com/api", result.url)
        assertEquals("POST", result.method)
        assertEquals("name=John Doe", result.body)
    }

    @Test
    fun `test parseCurlCommand with explicit url parameter`() {
        val curlString = "curl --url https://example.com/api"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com/api", result.url)
        assertEquals("GET", result.method)
    }

    @Test
    fun `test parseCurlCommand with user-agent parameter`() {
        val curlString = "curl -A \"Mozilla/5.0 (Windows NT 10.0; Win64; x64)\" https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("GET", result.method)
        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64)", result.headers["User-Agent"])
    }

    @Test
    fun `test parseCurlCommand with cookie parameter`() {
        val curlString = "curl -b \"session=abc123; user=john\" https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("GET", result.method)
        assertEquals("session=abc123; user=john", result.headers["Cookie"])
    }

    @Test
    fun `test parseCurlCommand with referer parameter`() {
        val curlString = "curl -e \"https://referrer.com\" https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("GET", result.method)
        assertEquals("https://referrer.com", result.headers["Referer"])
    }

    @Test
    fun `test parseCurlCommand with basic authentication`() {
        val curlString = "curl -u username:password https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("GET", result.method)
        val encodedAuth = java.util.Base64.getEncoder().encodeToString("username:password".toByteArray())
        assertEquals("Basic $encodedAuth", result.headers["Authorization"])
    }

    @Test
    fun `test parseCurlCommand with multiple body parameters`() {
        val curlString = "curl -d \"param1=value1\" -d \"param2=value2\" https://example.com/api"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com/api", result.url)
        assertEquals("POST", result.method)
        // Multiple body parameters should be combined with &
        assertEquals("param1=value1&param2=value2", result.body)
    }

    @Test
    fun `test parseCurlCommand with escaped characters in URL`() {
        val curlString = "curl \"https://example.com/api?q=escaped\\\"quote\\\"&param=value\""
        val result = CurlHelpers.parseCurlCommand(curlString)

        val expectedUrl = "https://example.com/api?q=escaped\"quote\"&param=value"
        assertEquals(expectedUrl, result.url)
        assertEquals("GET", result.method)
    }

    @Test
    fun `test parseCurlCommand with escaped characters in headers`() {
        val curlString =
            "curl -H \"X-Custom-Header: Value with escaped \\\"quotes\\\" and \\\\backslashes\" https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        val expectedValue = "Value with escaped \"quotes\" and \\backslashes"
        assertEquals(expectedValue, result.headers["X-Custom-Header"])
    }

    @Test
    fun `test parseCurlCommand with escaped characters in body`() {
        val curlString =
            "curl -d \"{\\\"key\\\":\\\"value with \\\\\\\"nested quotes\\\\\\\"\\\"}\" https://example.com/api"
        val result = CurlHelpers.parseCurlCommand(curlString)

        val expectedBody = "{\"key\":\"value with \\\"nested quotes\\\"\"}"
        assertEquals(expectedBody, result.body)
    }

    @Test
    fun `test parseCurlCommand with multiple URLs takes the first one`() {
        val curlString = "curl https://first-example.com https://second-example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://first-example.com", result.url)
        assertEquals("GET", result.method)
    }

    @Test
    fun `test parseCurlCommand with output options`() {
        val curlString = "curl -o output.txt https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("GET", result.method)
    }

    @Test
    fun `test parseCurlCommand with follow redirects option using -L`() {
        val curlString = "curl -L https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("GET", result.method)
        assertTrue(result.followRedirects, "followRedirects should be true when -L flag is used")
    }

    @Test
    fun `test parseCurlCommand with follow redirects option using --location`() {
        val curlString = "curl --location https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("GET", result.method)
        assertTrue(result.followRedirects, "followRedirects should be true when --location flag is used")
    }

    @Test
    fun `test parseCurlCommand with insecure option`() {
        val curlString = "curl -k https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("GET", result.method)
        // Note: The current implementation doesn't handle the -k flag
        // This test documents the current behavior
    }

    @Test
    fun `test parseCurlCommand with custom request method in lowercase`() {
        val curlString = "curl -X post https://example.com"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com", result.url)
        assertEquals("POST", result.method) // Method is now normalized to uppercase
    }

    @Test
    fun `test parseCurlCommand with data from file`() {
        val curlString = "curl -d @data.json https://example.com/api"
        val result = CurlHelpers.parseCurlCommand(curlString)

        assertEquals("https://example.com/api", result.url)
        assertEquals("POST", result.method)
        assertEquals("@data.json", result.body)
    }
}
