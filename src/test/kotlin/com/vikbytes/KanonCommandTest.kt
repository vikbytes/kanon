package com.vikbytes

import com.github.ajalt.clikt.core.UsageError
import kotlin.test.*
import org.junit.jupiter.api.Test

class KanonCommandTest {

    @Test
    fun `validateUrl should pass for valid http URLs`() {
        val command = KanonCommand()

        command.validateUrl("http://example.com")
        command.validateUrl("https://example.com")
        command.validateUrl("http://localhost:8080")
        command.validateUrl("https://127.0.0.1:9000")

        assertTrue(true)
    }

    @Test
    fun `validateUrl should fail for unresolvable domains`() {
        val command = KanonCommand()

        assertFailsWith<UsageError> { command.validateUrl("definitely-not-a-real-domain-12345.invalid") }
    }

    @Test
    fun `KanonCommand can be instantiated`() {
        val command = KanonCommand()
        assertNotNull(command)
    }

    @Test
    fun `validateUrl returns https URL for bare hostname`() {
        val command = KanonCommand()
        val result = command.validateUrl("localhost")
        assertEquals("https://localhost", result)
    }

    @Test
    fun `validateUrl returns https URL for bare hostname with port`() {
        val command = KanonCommand()
        val result = command.validateUrl("localhost:8080")
        assertEquals("https://localhost:8080", result)
    }

    @Test
    fun `validateUrl returns unchanged URL when already has http scheme`() {
        val command = KanonCommand()
        val result = command.validateUrl("http://example.com")
        assertEquals("http://example.com", result)
    }

    @Test
    fun `validateUrl returns unchanged URL when already has https scheme`() {
        val command = KanonCommand()
        val result = command.validateUrl("https://example.com/path?q=1")
        assertEquals("https://example.com/path?q=1", result)
    }
}
