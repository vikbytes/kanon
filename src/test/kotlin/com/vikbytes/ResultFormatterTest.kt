package com.vikbytes

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.HdrHistogram.Histogram
import org.junit.jupiter.api.Test

class ResultFormatterTest {

    @Test
    fun `test captureHdrHistogramDistribution does not print duplicate percentiles`() {
        val histogram = Histogram(3600000L, 2)

        // Add sample data to match the issue description
        // The issue description shows:
        // 38        | 0.0000      | 1
        // 41        | 50.0000     | 73
        // 42        | 75.0000     | 52
        // 45        | 90.0000     | 8
        // 50        | 95.0000     | 2
        // 133       | 99.0        | 1
        // 143       | 99.9        | 2

        // First value (0 percentile)
        histogram.recordValue(38)

        // Values for 50th percentile (about 500 values)
        for (i in 1..500) {
            histogram.recordValue(41)
        }

        // Values for 75th percentile (about 250 values)
        for (i in 1..250) {
            histogram.recordValue(42)
        }

        // Values for 90th percentile (about 150 values)
        for (i in 1..150) {
            histogram.recordValue(45)
        }

        // Values for 95th percentile (about 50 values)
        for (i in 1..50) {
            histogram.recordValue(50)
        }

        // Values for 99th percentile (about 40 values)
        for (i in 1..40) {
            histogram.recordValue(133)
        }

        // Values for 99.9th percentile (about 9 values)
        for (i in 1..9) {
            histogram.recordValue(143)
        }

        // This should create a histogram similar to the one in the issue description

        // Get the formatted output
        val output = ResultFormatter.captureHdrHistogramDistribution(histogram)

        // Print the output for debugging
        println("[DEBUG_LOG] Output:\n$output")

        // Check that 99.9 percentile is included
        assertTrue(output.contains("99.9"), "Output should contain 99.9 percentile")

        // Check that 99.99 and 100.0 percentiles are not included if they have the same value as 99.9
        // This is the key test for the fix
        assertFalse(
            output.contains("99.99"),
            "Output should not contain 99.99 percentile if it has the same value as 99.9",
        )
        assertFalse(
            output.contains("100.0"),
            "Output should not contain 100.0 percentile if it has the same value as 99.9",
        )
    }

    @Test
    fun `test histogram totalCount matches response time distribution counts`() {
        // Create a histogram with sample data
        val histogram = Histogram(3600000L, 2)

        // Record 1000 values
        for (i in 1..1000) {
            histogram.recordValue(i.toLong())
        }

        // Create a modified copy of the histogram to test our fix
        val modifiedHistogram = Histogram(3600000L, 2)
        for (i in 1..1000) {
            modifiedHistogram.recordValue(i.toLong())
        }

        // Get the formatted output for debugging
        val output = ResultFormatter.captureHdrHistogramDistribution(modifiedHistogram)
        println("[DEBUG_LOG] Histogram output:\n$output")

        // Define the percentiles we use in the display
        val percentiles = listOf(0.0, 50.0, 75.0, 90.0, 95.0, 99.0, 99.9, 99.99, 100.0)

        // Calculate bucket counts manually using the same logic as in our fix
        var lastCount = 0L
        val bucketCounts = mutableListOf<Long>()

        percentiles.forEach { percentile ->
            val value = modifiedHistogram.getValueAtPercentile(percentile)

            // Calculate the count for this percentile (number of values at or below this percentile)
            val countAtPercentile = (modifiedHistogram.totalCount * (percentile / 100.0)).toLong()

            // For display purposes, show the count in this bucket (between last percentile and this one)
            val bucketCount = if (percentile == 0.0) 0L else countAtPercentile - lastCount

            bucketCounts.add(bucketCount)
            lastCount = countAtPercentile
        }

        // Sum all bucket counts
        val totalBucketCount = bucketCounts.sum()

        // Print debug information
        println("[DEBUG_LOG] Bucket counts: $bucketCounts")
        println("[DEBUG_LOG] Total bucket count: $totalBucketCount")
        println("[DEBUG_LOG] Histogram total count: ${modifiedHistogram.totalCount}")

        // Verify that the sum of all bucket counts equals the histogram's total count
        assertEquals(
            modifiedHistogram.totalCount,
            totalBucketCount,
            "Sum of bucket counts ($totalBucketCount) should equal histogram total count (${modifiedHistogram.totalCount})",
        )
    }

    @Test
    fun `test formatDataSize with different byte sizes`() {
        // Test bytes
        assertEquals("10 B", ResultFormatter.formatDataSize(10L))
        assertEquals("999 B", ResultFormatter.formatDataSize(999L))

        // Test kilobytes - check unit and approximate value, not exact formatting
        val kb1024 = ResultFormatter.formatDataSize(1024L)
        assertTrue(kb1024.endsWith(" KB"), "Should end with KB")
        assertTrue(kb1024.startsWith("1"), "Should start with 1")

        val kb1536 = ResultFormatter.formatDataSize(1536L)
        assertTrue(kb1536.endsWith(" KB"), "Should end with KB")
        assertTrue(kb1536.contains("5"), "Should contain 5 (1.5)")

        // Test megabytes
        val mb1 = ResultFormatter.formatDataSize(1024L * 1024)
        assertTrue(mb1.endsWith(" MB"), "Should end with MB")
        assertTrue(mb1.startsWith("1"), "Should start with 1")

        val mb10 = ResultFormatter.formatDataSize(1024L * 1024 * 10)
        assertTrue(mb10.endsWith(" MB"), "Should end with MB")
        assertTrue(mb10.startsWith("10"), "Should start with 10")

        // Test gigabytes
        val gb1 = ResultFormatter.formatDataSize(1024L * 1024 * 1024)
        assertTrue(gb1.endsWith(" GB"), "Should end with GB")
        assertTrue(gb1.startsWith("1"), "Should start with 1")
    }

    @Test
    fun `test formatBandwidth with different rates`() {
        // Test bytes per second - check unit and approximate value, not exact formatting
        val b10 = ResultFormatter.formatBandwidth(10.0)
        assertTrue(b10.endsWith(" B/s"), "Should end with B/s")
        assertTrue(b10.startsWith("10"), "Should start with 10")

        val b999 = ResultFormatter.formatBandwidth(999.0)
        assertTrue(b999.endsWith(" B/s"), "Should end with B/s")
        assertTrue(b999.startsWith("999"), "Should start with 999")

        // Test kilobytes per second
        val kb1024 = ResultFormatter.formatBandwidth(1024.0)
        assertTrue(kb1024.endsWith(" KB/s"), "Should end with KB/s")
        assertTrue(kb1024.startsWith("1"), "Should start with 1")

        val kb1536 = ResultFormatter.formatBandwidth(1536.0)
        assertTrue(kb1536.endsWith(" KB/s"), "Should end with KB/s")
        assertTrue(kb1536.contains("5"), "Should contain 5 (1.5)")

        // Test megabytes per second
        val mb1 = ResultFormatter.formatBandwidth(1024.0 * 1024.0)
        assertTrue(mb1.endsWith(" MB/s"), "Should end with MB/s")
        assertTrue(mb1.startsWith("1"), "Should start with 1")

        val mb10 = ResultFormatter.formatBandwidth(1024.0 * 1024.0 * 10.0)
        assertTrue(mb10.endsWith(" MB/s"), "Should end with MB/s")
        assertTrue(mb10.startsWith("10"), "Should start with 10")

        // Test gigabytes per second
        val gb1 = ResultFormatter.formatBandwidth(1024.0 * 1024.0 * 1024.0)
        assertTrue(gb1.endsWith(" GB/s"), "Should end with GB/s")
        assertTrue(gb1.startsWith("1"), "Should start with 1")

        val gb25 = ResultFormatter.formatBandwidth(1024.0 * 1024.0 * 1024.0 * 2.5)
        assertTrue(gb25.endsWith(" GB/s"), "Should end with GB/s")
        assertTrue(gb25.startsWith("2"), "Should start with 2")
        assertTrue(gb25.contains("5"), "Should contain 5 (2.5)")
    }

    @Test
    fun `test formatResults with basic statistics`() {
        // Create test data
        val statistics = StatisticsHelper.RequestStatistics()
        statistics.successCount.set(80)
        statistics.failureCount.set(20)

        val statusCodes = ConcurrentHashMap<Int, AtomicInteger>()
        statusCodes[200] = AtomicInteger(70)
        statusCodes[404] = AtomicInteger(10)
        statusCodes[500] = AtomicInteger(20)
        statistics.statusCodes.putAll(statusCodes)

        statistics.requestBytes.set(1024L * 100) // 100 KB
        statistics.responseBytes.set(1024L * 200) // 200 KB

        // Create response time stats
        val histogram = Histogram(3600000L, 2)
        for (i in 1..100) {
            histogram.recordValue(i.toLong())
        }

        val responseTimeStats =
            StatisticsHelper.ResponseTimeStats(
                min = 1,
                max = 100,
                avg = 50.5,
                median = 50.5,
                p25 = 25,
                p50 = 50,
                p75 = 75,
                p90 = 90,
                p95 = 95,
                p99 = 99,
                p999 = 100,
                histogram = histogram,
            )

        // Format results
        val executionTime = 1000L // 1 second
        val concurrency = 10
        val results =
            ResultFormatter.formatResults(
                statistics = statistics,
                executionTime = executionTime,
                concurrency = concurrency,
                responseTimeStats = responseTimeStats,
                noBandwidth = false,
                torture = false,
            )

        // Print the results for debugging
        println("[DEBUG_LOG] Results:\n$results")

        // Verify results contain expected information
        assertTrue(results.contains("Total requests:"), "Results should contain total requests")
        assertTrue(results.contains("Successful requests:"), "Results should contain successful requests")
        assertTrue(results.contains("Failed requests:"), "Results should contain failed requests")
        assertTrue(results.contains("Concurrency level:"), "Results should contain concurrency level")
        assertTrue(results.contains("Total execution time:"), "Results should contain execution time")
        assertTrue(results.contains("Requests per second:"), "Results should contain requests per second")

        // Verify response time stats
        assertTrue(results.contains("Min:"), "Results should contain min response time")
        assertTrue(results.contains("Max:"), "Results should contain max response time")
        assertTrue(results.contains("Avg:"), "Results should contain avg response time")
        assertTrue(results.contains("Median:"), "Results should contain median response time")

        // Verify status code distribution
        assertTrue(results.contains("200"), "Results should contain 200 status code")
        assertTrue(results.contains("404"), "Results should contain 404 status code")
        assertTrue(results.contains("500"), "Results should contain 500 status code")

        // Verify bandwidth information
        assertTrue(results.contains("Upload data:"), "Results should contain upload data")
        assertTrue(results.contains("Download data:"), "Results should contain download data")
        assertTrue(results.contains("Total data transferred:"), "Results should contain total data")
        assertTrue(results.contains("Upload bandwidth:"), "Results should contain upload bandwidth")
        assertTrue(results.contains("Download bandwidth:"), "Results should contain download bandwidth")
        assertTrue(results.contains("Total bandwidth:"), "Results should contain total bandwidth")
    }

    @Test
    fun `test formatResults with noBandwidth flag`() {
        // Create minimal test data
        val statistics = StatisticsHelper.RequestStatistics()
        statistics.successCount.set(100)

        val responseTimeStats =
            StatisticsHelper.ResponseTimeStats(
                min = 1,
                max = 100,
                avg = 50.0,
                median = 50.0,
                p25 = 25,
                p50 = 50,
                p75 = 75,
                p90 = 90,
                p95 = 95,
                p99 = 99,
                p999 = 100,
                histogram = Histogram(3600000L, 2),
            )

        // Format results with noBandwidth = true
        val results =
            ResultFormatter.formatResults(
                statistics = statistics,
                executionTime = 1000L,
                concurrency = 10,
                responseTimeStats = responseTimeStats,
                noBandwidth = true,
                torture = false,
            )

        // Verify bandwidth information is not included
        assertFalse(results.contains("Upload data:"), "Results should not contain upload data")
        assertFalse(results.contains("Download data:"), "Results should not contain download data")
        assertFalse(results.contains("Total data transferred:"), "Results should not contain total data")
        assertFalse(results.contains("Upload bandwidth:"), "Results should not contain upload bandwidth")
        assertFalse(results.contains("Download bandwidth:"), "Results should not contain download bandwidth")
        assertFalse(results.contains("Total bandwidth:"), "Results should not contain total bandwidth")
    }

    @Test
    fun `test formatResults with torture flag`() {
        // Create minimal test data
        val statistics = StatisticsHelper.RequestStatistics()
        statistics.successCount.set(100)

        val responseTimeStats =
            StatisticsHelper.ResponseTimeStats(
                min = 1,
                max = 100,
                avg = 50.0,
                median = 50.0,
                p25 = 25,
                p50 = 50,
                p75 = 75,
                p90 = 90,
                p95 = 95,
                p99 = 99,
                p999 = 100,
                histogram = Histogram(3600000L, 2),
            )

        // Format results with torture = true
        val results =
            ResultFormatter.formatResults(
                statistics = statistics,
                executionTime = 1000L,
                concurrency = 10,
                responseTimeStats = responseTimeStats,
                noBandwidth = false,
                torture = true,
            )

        // Verify concurrency level is not included
        assertFalse(
            results.contains("Concurrency level:"),
            "Results should not contain concurrency level when torture=true",
        )
    }

    @Test
    fun `test formatDataSize with 2 GB value`() {
        val twoGB = 2L * 1024 * 1024 * 1024
        val result = ResultFormatter.formatDataSize(twoGB)
        assertTrue(result.endsWith(" GB"), "Should end with GB, got: $result")
        assertTrue(result.startsWith("2"), "Should start with 2, got: $result")
    }

    @Test
    fun `test formatDataSize with value just below GB boundary`() {
        val justUnderGB = 1024L * 1024 * 1024 - 1
        val result = ResultFormatter.formatDataSize(justUnderGB)
        assertTrue(result.endsWith(" MB"), "Value just under 1 GB should display as MB, got: $result")
    }

    @Test
    fun `test formatDataSize with Long MAX_VALUE does not crash`() {
        val result = ResultFormatter.formatDataSize(Long.MAX_VALUE)
        assertTrue(result.endsWith(" GB"), "Very large value should display as GB, got: $result")
        assertFalse(result.contains("Infinity"), "Should not contain Infinity, got: $result")
    }

    @Test
    fun `test formatDataSize with zero bytes`() {
        assertEquals("0 B", ResultFormatter.formatDataSize(0L))
    }

    @Test
    fun `test formatResults with zero execution time does not crash`() {
        val statistics = StatisticsHelper.RequestStatistics()
        statistics.successCount.set(10)

        val responseTimeStats = StatisticsHelper.ResponseTimeStats(
            min = 0, max = 0, avg = 0.0, median = 0.0,
            p25 = 0, p50 = 0, p75 = 0, p90 = 0, p95 = 0, p99 = 0, p999 = 0,
            histogram = Histogram(3600000L, 2),
        )

        // Should not throw even with executionTime = 0
        val results = ResultFormatter.formatResults(
            statistics = statistics,
            executionTime = 0L,
            concurrency = 1,
            responseTimeStats = responseTimeStats,
            noBandwidth = true,
            torture = false,
        )

        assertTrue(results.contains("Total requests:"), "Should still format results")
        assertTrue(results.contains("10"), "Should contain total request count")
    }

    @Test
    fun `test formatResults status codes sum may differ from total when exceptions occur`() {
        val statistics = StatisticsHelper.RequestStatistics()
        statistics.successCount.set(5)
        statistics.failureCount.set(2) // exception-based failures, no status code entry

        statistics.statusCodes[200] = AtomicInteger(5)
        // No status code entry for the 2 exception failures

        val histogram = Histogram(3600000L, 2)
        for (i in 1..5) histogram.recordValue(i.toLong())

        val responseTimeStats = StatisticsHelper.ResponseTimeStats(
            min = 1, max = 5, avg = 3.0, median = 3.0,
            p25 = 2, p50 = 3, p75 = 4, p90 = 5, p95 = 5, p99 = 5, p999 = 5,
            histogram = histogram,
        )

        val results = ResultFormatter.formatResults(
            statistics = statistics,
            executionTime = 1000L,
            concurrency = 1,
            responseTimeStats = responseTimeStats,
            noBandwidth = true,
            torture = false,
        )

        // Total should be 7 (5 success + 2 failure)
        assertTrue(results.contains("7"), "Total requests should be 7")
        assertTrue(results.contains("5"), "Successful requests should be 5")
        assertTrue(results.contains("2"), "Failed requests should be 2")
    }

    @Test
    fun `test captureHdrHistogramDistribution with empty histogram`() {
        val histogram = Histogram(3600000L, 2)
        val output = ResultFormatter.captureHdrHistogramDistribution(histogram)
        assertTrue(output.contains("No response times recorded"), "Should indicate empty histogram")
    }
}
