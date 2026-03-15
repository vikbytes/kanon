package com.vikbytes

import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.testing.test
import org.junit.jupiter.api.Test
import kotlin.test.*

class KanonCommandValidationTest {

    @Test
    fun `validateArguments should fail when both number and duration are provided`() {
        val command = KanonCommand()

        val result = command.test("--url https://example.com --number 10 --duration 5")

        assertEquals(1, result.statusCode)
        assertTrue(result.output.contains("You must provide either"))
        assertTrue(result.output.contains("--number"))
        assertTrue(result.output.contains("--duration"))
        assertTrue(result.output.contains("but not both"))
    }

    @Test
    fun `validateArguments should fail when neither number nor duration are provided`() {
        val command = KanonCommand()

        val result = command.test("--url https://example.com")

        assertEquals(1, result.statusCode)
        assertTrue(result.output.contains("You must provide either"))
        assertTrue(result.output.contains("--number"))
        assertTrue(result.output.contains("--duration"))
        assertTrue(result.output.contains("but not both"))
    }

    @Test
    fun `validateArguments should fail when both curl and curl-file are provided`() {
        val command = KanonCommand()

        val result = command.test("--curl 'curl https://example.com' --curl-file test.txt --number 10")

        assertEquals(1, result.statusCode)
        assertTrue(result.output.contains("You must provide either"))
        assertTrue(result.output.contains("--curl"))
        assertTrue(result.output.contains("--curl-file"))
        assertTrue(result.output.contains("but not both"))
    }

    @Test
    fun `validateArguments should fail when curl is mixed with url option`() {
        val command = KanonCommand()

        val result = command.test("--curl 'curl https://example.com' --url https://other.com --number 10")

        assertEquals(1, result.statusCode)
        assertTrue(result.output.contains("When using"))
        assertTrue(result.output.contains("--curl"))
        assertTrue(result.output.contains("you cannot use"))
        assertTrue(result.output.contains("--url"))
    }

    @Test
    fun `validateArguments should fail when curl is mixed with headers option`() {
        val command = KanonCommand()

        val result = command.test("--curl 'curl https://example.com' --headers 'Content-Type: application/json' --number 10")

        assertEquals(1, result.statusCode)
        assertTrue(result.output.contains("When using"))
        assertTrue(result.output.contains("--curl"))
        assertTrue(result.output.contains("you cannot use"))
        assertTrue(result.output.contains("--headers"))
    }

    @Test
    fun `validateArguments should fail when curl is mixed with authorization option`() {
        val command = KanonCommand()

        val result = command.test("--curl 'curl https://example.com' --authorization 'Bearer token' --number 10")

        assertEquals(1, result.statusCode)
        assertTrue(result.output.contains("When using"))
        assertTrue(result.output.contains("--curl"))
        assertTrue(result.output.contains("you cannot use"))
        assertTrue(result.output.contains("--authorization"))
    }

    @Test
    fun `validateArguments should fail when curl is mixed with body option`() {
        val command = KanonCommand()

        val result = command.test("--curl 'curl https://example.com' --body '{\"test\": true}' --number 10")

        assertEquals(1, result.statusCode)
        assertTrue(result.output.contains("When using"))
        assertTrue(result.output.contains("--curl"))
        assertTrue(result.output.contains("you cannot use"))
        assertTrue(result.output.contains("--body"))
    }

    @Test
    fun `validateArguments should fail when curl is mixed with method option`() {
        val command = KanonCommand()

        val result = command.test("--curl 'curl https://example.com' --method POST --number 10")

        assertEquals(1, result.statusCode)
        assertTrue(result.output.contains("When using"))
        assertTrue(result.output.contains("--curl"))
        assertTrue(result.output.contains("you cannot use"))
        assertTrue(result.output.contains("--method"))
    }

    @Test
    fun `validateArguments should fail when curl-file is mixed with url option`() {
        val command = KanonCommand()

        val result = command.test("--curl-file test.txt --url https://example.com --number 10")

        assertEquals(1, result.statusCode)
        assertTrue(result.output.contains("When using"))
        assertTrue(result.output.contains("--curl-file"))
        assertTrue(result.output.contains("you cannot use"))
        assertTrue(result.output.contains("--url"))
    }

    @Test
    fun `validateArguments should pass with valid number option`() {
        val command = KanonCommand()

        // This should not fail validation (though it may fail later due to missing implementation)
        val result = command.test("--url https://httpbin.org/get --number 1")

        // Should not fail with validation error
        assertFalse(result.output.contains("You must provide either"))
        assertFalse(result.output.contains("When using"))
    }

    @Test
    fun `validateArguments should pass with valid duration option`() {
        val command = KanonCommand()

        val result = command.test("--url https://httpbin.org/get --duration 1")

        // Should not fail with validation error
        assertFalse(result.output.contains("You must provide either"))
        assertFalse(result.output.contains("When using"))
    }

    @Test
    fun `validateArguments should pass with valid curl option`() {
        val command = KanonCommand()

        val result = command.test("--curl 'curl https://httpbin.org/get' --number 1")

        // Should not fail with validation error
        assertFalse(result.output.contains("You must provide either"))
        assertFalse(result.output.contains("When using"))
        assertFalse(result.output.contains("you cannot use"))
    }
}