package com.vikbytes

import java.io.File
import kotlin.test.*
import org.junit.jupiter.api.Test

class ResultFormatterFileTest {

    @Test
    fun `saveResultsToFile creates file with correct content`() {
        val url = "https://example.com"
        val results = "Test results with ${TerminalColors.RED}colors${TerminalColors.RESET}"

        ResultFormatter.saveResultsToFile(url, results)

        val files = File(".").listFiles { _, name -> name.startsWith("kanon-example_com-") && name.endsWith(".txt") }

        try {
            assertNotNull(files, "Should find created files")
            assertTrue(files.isNotEmpty(), "Should create at least one file")

            val file = files.first()
            assertTrue(file.exists(), "File should exist")

            val content = file.readText()
            assertEquals("Test results with colors", content, "File content should have ANSI codes stripped")
        } finally {
            files?.forEach { it.delete() }
        }
    }

    @Test
    fun `saveResultsToFile handles URLs with special characters`() {
        val url = "https://api.example.com:8080/path?param=value"
        val results = "Simple test results"

        ResultFormatter.saveResultsToFile(url, results)

        val files =
            File(".").listFiles { _, name ->
                name.startsWith("kanon-api_example_com-8080-path-param-value-") && name.endsWith(".txt")
            }

        try {
            assertNotNull(files, "Should find created files")
            assertTrue(files.isNotEmpty(), "Should create file with cleaned URL name")

            val file = files.first()
            assertTrue(file.exists(), "File should exist")
            assertEquals("Simple test results", file.readText(), "File content should match input")
        } finally {
            files?.forEach { it.delete() }
        }
    }

    @Test
    fun `saveResultsToFile handles empty results`() {
        val url = "https://test.com"
        val results = ""

        ResultFormatter.saveResultsToFile(url, results)

        val files = File(".").listFiles { _, name -> name.startsWith("kanon-test_com-") && name.endsWith(".txt") }

        try {
            assertNotNull(files, "Should find created files")
            assertTrue(files.isNotEmpty(), "Should create file even with empty content")

            val file = files.first()
            assertTrue(file.exists(), "File should exist")
            assertEquals("", file.readText(), "File should be empty")
        } finally {
            files?.forEach { it.delete() }
        }
    }
}
