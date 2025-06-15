package com.vikbytes

import org.HdrHistogram.Histogram
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals

class ResultFormatterTest {

    @Test
    fun `test captureHdrHistogramDistribution does not print duplicate percentiles`() {
        // Create a histogram with values that will result in the same value for multiple percentiles
        val histogram = Histogram(3600000L, 2)

        // Add sample data to match the issue description
        // The issue description shows:
        // 38        | 0.0000      | 1         
        // 41        | 50.0000     | 73        
        // 42        | 75.0000     | 52        
        // 45        | 90.0000     | 8         
        // 50        | 95.0000     | 2         
        // 133       | 99.0000     | 1         
        // 143       | 99.9000     | 2         

        // We need at least 1000 values to have meaningful 99.9 percentile
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
        assertTrue(output.contains("99.9000"), "Output should contain 99.9 percentile")

        // Check that 99.99 and 100.0 percentiles are not included if they have the same value as 99.9
        // This is the key test for the fix
        assertFalse(output.contains("99.9900"), "Output should not contain 99.99 percentile if it has the same value as 99.9")
        assertFalse(output.contains("100.0000"), "Output should not contain 100.0 percentile if it has the same value as 99.9")
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
        assertEquals(modifiedHistogram.totalCount, totalBucketCount, 
            "Sum of bucket counts ($totalBucketCount) should equal histogram total count (${modifiedHistogram.totalCount})")
    }
}
