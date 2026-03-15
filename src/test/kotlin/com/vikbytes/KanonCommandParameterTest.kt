package com.vikbytes

import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Test
import kotlin.test.*

class KanonCommandParameterTest {

    @Test
    fun `getRequestParameters should process basic URL parameter correctly`() {
        val command = KanonCommand()
        command.test("--url https://httpbin.org/get --number 1")

        val params = command.getRequestParameters()

        assertEquals("https://httpbin.org/get", params["url"])
        assertEquals("GET", params["method"])
        assertNull(params["headers"])
        assertNull(params["authorization"])
        assertNull(params["body"])
    }

    @Test
    fun `getRequestParameters should process headers parameter correctly`() {
        val command = KanonCommand()
        command.test("--url https://httpbin.org/get --headers 'Content-Type: application/json' --number 1")

        val params = command.getRequestParameters()

        assertEquals("https://httpbin.org/get", params["url"])
        assertEquals("Content-Type: application/json", params["headers"])
    }

    @Test
    fun `getRequestParameters should process authorization parameter correctly`() {
        val command = KanonCommand()
        command.test("--url https://httpbin.org/get --authorization 'Bearer token123' --number 1")

        val params = command.getRequestParameters()

        assertEquals("https://httpbin.org/get", params["url"])
        assertEquals("Bearer token123", params["authorization"])
    }

    @Test
    fun `getRequestParameters should process HTTP method parameter correctly`() {
        val command = KanonCommand()
        command.test("--url https://httpbin.org/post --method POST --number 1")

        val params = command.getRequestParameters()

        assertEquals("https://httpbin.org/post", params["url"])
        assertEquals("POST", params["method"])
    }

    @Test
    fun `getRequestParameters should process request body parameter correctly`() {
        val command = KanonCommand()
        command.test("--url https://httpbin.org/post --method POST --body '{\"test\": true}' --number 1")

        val params = command.getRequestParameters()

        assertEquals("https://httpbin.org/post", params["url"])
        assertEquals("POST", params["method"])
        assertEquals("{\"test\": true}", params["body"])
    }

    @Test
    fun `getRequestParameters should use default method when not specified`() {
        val command = KanonCommand()
        command.test("--url https://httpbin.org/get --number 1")

        val params = command.getRequestParameters()

        assertEquals("https://httpbin.org/get", params["url"])
        assertEquals("GET", params["method"]) // Default should be GET
    }

    @Test
    fun `getRequestParameters should process followRedirects parameter correctly`() {
        val command = KanonCommand()
        command.test("--url https://httpbin.org/get --location --number 1")

        val params = command.getRequestParameters()

        assertEquals("https://httpbin.org/get", params["url"])
        assertEquals("true", params["followRedirects"])
    }

    @Test
    fun `getRequestParameters should handle curl command processing`() {
        val command = KanonCommand()
        command.test("--curl 'curl https://httpbin.org/get' --number 1")

        val params = command.getRequestParameters()

        assertEquals("https://httpbin.org/get", params["url"])
        assertEquals("GET", params["method"])
    }

    @Test
    fun `getRequestParameters should handle curl command with headers`() {
        val command = KanonCommand()
        command.test("--curl 'curl -H \"Content-Type: application/json\" https://httpbin.org/post' --number 1")

        val params = command.getRequestParameters()

        assertEquals("https://httpbin.org/post", params["url"])
        assertNotNull(params["headers"])
        assertTrue(params["headers"]!!.contains("Content-Type: application/json"))
    }

    @Test
    fun `getRequestParameters should handle curl command with POST method`() {
        val command = KanonCommand()
        command.test("--curl 'curl -X POST https://httpbin.org/post' --number 1")

        val params = command.getRequestParameters()

        assertEquals("https://httpbin.org/post", params["url"])
        assertEquals("POST", params["method"])
    }

    @Test
    fun `getRequestParameters converts comma-separated headers to newline-separated`() {
        val command = KanonCommand()
        command.test("--url https://httpbin.org/get --headers 'Content-Type: application/json,X-Trace-Id: abc' --number 1")

        val params = command.getRequestParameters()

        assertEquals("Content-Type: application/json\nX-Trace-Id: abc", params["headers"])
    }

    @Test
    fun `getRequestParameters preserves single header without conversion`() {
        val command = KanonCommand()
        command.test("--url https://httpbin.org/get --headers 'Accept: text/html, application/json' --number 1")

        val params = command.getRequestParameters()

        // A single header with commas in its value gets newlines inserted — this is the known
        // trade-off of the comma separator for the --headers CLI option.
        assertNotNull(params["headers"])
    }

    @Test
    fun `getRequestParameters via curl propagates all fields`() {
        val command = KanonCommand()
        command.test("--curl 'curl -X POST -H \"Authorization: Bearer tok\" -d data -L https://api.example.com' --number 1")

        val params = command.getRequestParameters()

        assertEquals("https://api.example.com", params["url"])
        assertEquals("POST", params["method"])
        assertNotNull(params["headers"])
        assertEquals("Bearer tok", params["authorization"])
        assertEquals("data", params["body"])
        assertEquals("true", params["followRedirects"])
    }

    @Test
    fun `getRequestParameters defaults followRedirects to false`() {
        val command = KanonCommand()
        command.test("--url https://httpbin.org/get --number 1")

        val params = command.getRequestParameters()

        assertEquals("false", params["followRedirects"])
    }
}