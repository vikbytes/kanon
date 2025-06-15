package com.vikbytes

import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import org.HdrHistogram.Histogram

object StatisticsHelper {

    data class RequestStatistics(
        val successCount: AtomicInteger = AtomicInteger(0),
        val failureCount: AtomicInteger = AtomicInteger(0),
        val responseTimes: MutableList<Long> = Collections.synchronizedList(mutableListOf()),
        val statusCodes: ConcurrentHashMap<Int, AtomicInteger> = ConcurrentHashMap(),
        val requestBytes: AtomicInteger = AtomicInteger(0),
        val responseBytes: AtomicInteger = AtomicInteger(0),
        val histogram: Histogram = Histogram(3600000L, 2)
    )

    data class ResponseTimeStats(
        val min: Long,
        val max: Long,
        val avg: Double,
        val median: Double,
        val p50: Long,
        val p75: Long,
        val p90: Long,
        val p95: Long,
        val p99: Long,
        val p999: Long,
        val histogram: Histogram
    )

    fun calculateResponseTimeStats(responseTimes: List<Long>): ResponseTimeStats {
        val sortedTimes = responseTimes.sorted()
        val totalResponses = sortedTimes.size

        val min = sortedTimes.firstOrNull() ?: 0
        val max = sortedTimes.lastOrNull() ?: 0
        val avg = if (totalResponses > 0) sortedTimes.average() else 0.0

        val median =
            if (totalResponses > 0) {
                if (totalResponses % 2 == 0) {
                    (sortedTimes[totalResponses / 2 - 1].toDouble() + sortedTimes[totalResponses / 2].toDouble()) / 2.0
                } else {
                    sortedTimes[totalResponses / 2].toDouble()
                }
            } else 0.0

        val p50 =
            if (totalResponses > 0) sortedTimes[(totalResponses * 0.50).toInt().coerceAtMost(totalResponses - 1)] else 0
        val p75 =
            if (totalResponses > 0) sortedTimes[(totalResponses * 0.75).toInt().coerceAtMost(totalResponses - 1)] else 0
        val p90 =
            if (totalResponses > 0) sortedTimes[(totalResponses * 0.90).toInt().coerceAtMost(totalResponses - 1)] else 0
        val p95 =
            if (totalResponses > 0) sortedTimes[(totalResponses * 0.95).toInt().coerceAtMost(totalResponses - 1)] else 0
        val p99 =
            if (totalResponses > 0) sortedTimes[(totalResponses * 0.99).toInt().coerceAtMost(totalResponses - 1)] else 0
        val p999 =
            if (totalResponses > 0) sortedTimes[(totalResponses * 0.999).toInt().coerceAtMost(totalResponses - 1)]
            else 0

        val histogram = Histogram(3600000L, 2)
        responseTimes.forEach { histogram.recordValue(it) }

        return ResponseTimeStats(min, max, avg, median, p50, p75, p90, p95, p99, p999, histogram)
    }
}
