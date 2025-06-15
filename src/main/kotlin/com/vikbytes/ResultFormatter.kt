package com.vikbytes

import java.io.File
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ResultFormatter {

    fun formatResults(
        statistics: StatisticsHelper.RequestStatistics,
        executionTime: Long,
        concurrency: Int,
        responseTimeStats: StatisticsHelper.ResponseTimeStats,
        noBandwidth: Boolean = false,
        torture: Boolean = false
    ): String {
        val results = StringBuilder()
        results.appendLine("Summary:".boldYellow())
        val totalRequestsMade = statistics.successCount.get() + statistics.failureCount.get()

        results.appendLine("Total requests: ".bold() + totalRequestsMade.toString().cyan())
        results.appendLine("Successful requests: ".bold() + statistics.successCount.get().toString().green())
        results.appendLine(
            "Failed requests: ".bold() +
                statistics.failureCount.get().toString().let {
                    if (statistics.failureCount.get() > 0) it.red() else it.green()
                })
        if (!torture) {
            results.appendLine("Concurrency level: ".bold() + concurrency.toString().cyan())
        }
        results.appendLine("Total execution time: ".bold() + "${executionTime}ms".cyan())
        results.appendLine(
            "Requests per second: ".bold() + "${(totalRequestsMade * 1000.0 / executionTime).toInt()}".boldCyan())

        results.appendLine("\n" + "Response time statistics (ms):".boldYellow())
        results.appendLine("Min: ".bold() + "${responseTimeStats.min}".green())
        results.appendLine("Max: ".bold() + "${responseTimeStats.max}".red())
        results.appendLine("Avg: ".bold() + "${responseTimeStats.avg.toInt()}".cyan())
        results.appendLine("Median: ".bold() + "${responseTimeStats.median.toInt()}".cyan())

        results.appendLine("\n" + "Status code distribution:".boldYellow())
        statistics.statusCodes.entries
            .sortedBy { entry -> entry.key }
            .forEach { entry ->
                val statusColor =
                    when {
                        entry.key < 300 -> "green"
                        entry.key < 400 -> "cyan"
                        entry.key < 500 -> "yellow"
                        else -> "red"
                    }
                val coloredStatus =
                    when (statusColor) {
                        "green" -> entry.key.toString().green()
                        "cyan" -> entry.key.toString().cyan()
                        "yellow" -> entry.key.toString().yellow()
                        else -> entry.key.toString().red()
                    }
                results.appendLine("$coloredStatus: ${entry.value.get()}")
            }

        val hdrDistributionOutput = captureHdrHistogramDistribution(responseTimeStats.histogram)
        results.append(hdrDistributionOutput)

        if (!noBandwidth) {
            results.appendLine("\n" + "Data transfer:".boldYellow())
            val totalRequestBytes = statistics.requestBytes.get()
            val totalResponseBytes = statistics.responseBytes.get()
            val totalBytes = totalRequestBytes + totalResponseBytes

            val uploadBandwidthBytesPerSecond =
                if (executionTime > 0) totalRequestBytes / (executionTime / 1000.0) else 0.0
            val downloadBandwidthBytesPerSecond =
                if (executionTime > 0) totalResponseBytes / (executionTime / 1000.0) else 0.0
            val totalBandwidthBytesPerSecond = if (executionTime > 0) totalBytes / (executionTime / 1000.0) else 0.0

            results.appendLine("Upload data: ".bold() + formatDataSize(totalRequestBytes).cyan())
            results.appendLine("Download data: ".bold() + formatDataSize(totalResponseBytes).cyan())
            results.appendLine("Total data transferred: ".bold() + formatDataSize(totalBytes).boldCyan())
            results.appendLine(
                "Average data per request: ".bold() +
                    formatDataSize(if (totalRequestsMade > 0) totalBytes / totalRequestsMade else 0).cyan())
            results.appendLine("Upload bandwidth: ".bold() + formatBandwidth(uploadBandwidthBytesPerSecond).cyan())
            results.appendLine("Download bandwidth: ".bold() + formatBandwidth(downloadBandwidthBytesPerSecond).cyan())
            results.appendLine("Total bandwidth: ".bold() + formatBandwidth(totalBandwidthBytesPerSecond).boldCyan())
        }

        return results.toString()
    }

    fun captureResponseTimeDistribution(responseTimes: List<Long>): String {
        val output = StringBuilder()

        if (responseTimes.isEmpty()) {
            output.appendLine("No response times recorded.".italic().yellow())
            return output.toString()
        }

        val min = responseTimes.minOrNull() ?: 0
        val max = responseTimes.maxOrNull() ?: 0

        val numBuckets = 10
        val bucketSize = if (max > min) (max - min + 1) / numBuckets.toDouble() else 1.0

        val buckets = IntArray(numBuckets)
        val bucketLabels = Array(numBuckets) { "" }

        responseTimes.forEach { time ->
            val bucketIndex =
                if (bucketSize > 0) {
                    ((time - min) / bucketSize).toInt().coerceIn(0, numBuckets - 1)
                } else {
                    0
                }
            buckets[bucketIndex]++
        }

        val maxCount = buckets.maxOrNull() ?: 0
        if (maxCount == 0) {
            output.appendLine("No data to display.".italic().yellow())
            return output.toString()
        }

        val maxBarLength = 40
        val scaleFactor = maxBarLength.toDouble() / maxCount

        for (i in 0 until numBuckets) {
            val lowerBound = min + (i * bucketSize).toLong()
            val upperBound = min + ((i + 1) * bucketSize).toLong() - 1
            bucketLabels[i] =
                if (i == numBuckets - 1) {
                    "≤${max}"
                } else {
                    "≤${upperBound}"
                }
        }

        val maxLabelLength = bucketLabels.maxOfOrNull { it.length } ?: 0

        for (i in 0 until numBuckets) {
            val count = buckets[i]
            val barLength = (count * scaleFactor).toInt()

            val barColor =
                when (i) {
                    0,
                    1,
                    2 -> TerminalColors.GREEN
                    3,
                    4,
                    5 -> TerminalColors.CYAN
                    6,
                    7 -> TerminalColors.YELLOW
                    else -> TerminalColors.RED
                }

            val bar = "$barColor${"█".repeat(barLength)}${TerminalColors.RESET}"
            val paddedLabel = bucketLabels[i].padEnd(maxLabelLength).bold()
            output.appendLine("  $paddedLabel | $bar (${count.toString().cyan()})")
        }

        return output.toString()
    }

    fun captureHdrHistogramDistribution(histogram: org.HdrHistogram.Histogram): String {
        val output = StringBuilder()

        if (histogram.totalCount == 0L) {
            output.appendLine("No response times recorded in histogram.".italic().yellow())
            return output.toString()
        }

        output.appendLine()
        output.appendLine("HDR Histogram Distribution (ms):".boldYellow())

        // Define the percentiles to display
        val percentiles = listOf(25.0, 50.0, 75.0, 90.0, 95.0, 99.0, 99.9, 99.99, 100.0)

        var lastValue = -1L
        var lastCount = 0L

        // Find the maximum count for scaling the bars
        val maxCount = histogram.totalCount
        val maxBarLength = 40
        val scaleFactor = if (maxCount > 0) maxBarLength.toDouble() / maxCount else 0.0

        percentiles.forEach { percentile ->
            val value = histogram.getValueAtPercentile(percentile)

            // Only print this percentile if its value is different from the previous one
            // This avoids printing higher percentiles when they have the same value as lower ones
            if (value != lastValue || percentile == 0.0) {
                // Calculate the count for this percentile (number of values at or below this percentile)
                val countAtPercentile = (histogram.totalCount * (percentile / 100.0)).toLong()

                // For display purposes, show the count in this bucket (between last percentile and this one)
                val bucketCount = if (percentile == 0.0) 0L else countAtPercentile - lastCount

                val valueStr = "${value}ms".padEnd(9)
                val percentileStr = percentile.toString().padEnd(11)

                // Calculate bar length based on bucket count
                val barLength = (bucketCount * scaleFactor).toInt().coerceAtLeast(1)

                // Determine bar color based on percentile
                val barColor =
                    when {
                        percentile <= 50.0 -> TerminalColors.GREEN
                        percentile <= 90.0 -> TerminalColors.YELLOW
                        else -> TerminalColors.RED
                    }

                // Create the bar
                val bar = "$barColor${"█".repeat(barLength)}${TerminalColors.RESET}"

                val line = "$valueStr | $percentileStr | $bar (${bucketCount.toString().cyan()})"

                val coloredLine =
                    when {
                        percentile <= 50.0 -> line.green()
                        percentile <= 90.0 -> line.yellow()
                        else -> line.red()
                    }

                output.appendLine(coloredLine)
                lastValue = value
                lastCount = countAtPercentile
            }
        }

        return output.toString()
    }

    fun saveResultsToFile(url: String, results: String) {
        try {
            val cleanUrl = url.replace(Regex("^https?://"), "").replace(".", "_").replace(Regex("[^a-zA-Z0-9_-]"), "-")
            val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"))
            val filename = "kanon-$cleanUrl-$timestamp.txt"

            val plainResults = stripAnsiCodes(results)
            File(filename).writeText(plainResults)

            println("\n" + "Results for URL '$url' saved to file: ".green() + filename.boldGreen())
        } catch (e: Exception) {
            println("\n" + "Error saving results to file: ".boldRed() + "${e.message}".red())
        }
    }

    fun formatDataSize(bytes: Int): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    fun formatBandwidth(bytesPerSecond: Double): String {
        return when {
            bytesPerSecond < 1024 -> String.format("%.2f B/s", bytesPerSecond)
            bytesPerSecond < 1024 * 1024 -> String.format("%.2f KB/s", bytesPerSecond / 1024.0)
            bytesPerSecond < 1024 * 1024 * 1024 -> String.format("%.2f MB/s", bytesPerSecond / (1024.0 * 1024.0))
            else -> String.format("%.2f GB/s", bytesPerSecond / (1024.0 * 1024.0 * 1024.0))
        }
    }
}
