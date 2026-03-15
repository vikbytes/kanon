package com.vikbytes

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import org.HdrHistogram.Histogram

object StatisticsHelper {

    data class RequestStatistics(
        val successCount: AtomicInteger = AtomicInteger(0),
        val failureCount: AtomicInteger = AtomicInteger(0),
        val responseTimes: ConcurrentLinkedQueue<Long> = ConcurrentLinkedQueue(),
        val statusCodes: ConcurrentHashMap<Int, AtomicInteger> = ConcurrentHashMap(),
        val requestBytes: AtomicLong = AtomicLong(0),
        val responseBytes: AtomicLong = AtomicLong(0),
        val histogram: Histogram = Histogram(3600000L, 2)
    )

    data class ResponseTimeStats(
        val min: Long,
        val max: Long,
        val avg: Double,
        val median: Double,
        val p25: Long,
        val p50: Long,
        val p75: Long,
        val p90: Long,
        val p95: Long,
        val p99: Long,
        val p999: Long,
        val histogram: Histogram
    )

    fun calculateResponseTimeStats(histogram: Histogram): ResponseTimeStats {
        val min = if (histogram.totalCount > 0) histogram.minValue else 0
        val max = if (histogram.totalCount > 0) histogram.maxValue else 0
        val avg = histogram.mean
        val median = histogram.getValueAtPercentile(50.0).toDouble()

        return ResponseTimeStats(
            min = min,
            max = max,
            avg = avg,
            median = median,
            p25 = histogram.getValueAtPercentile(25.0),
            p50 = histogram.getValueAtPercentile(50.0),
            p75 = histogram.getValueAtPercentile(75.0),
            p90 = histogram.getValueAtPercentile(90.0),
            p95 = histogram.getValueAtPercentile(95.0),
            p99 = histogram.getValueAtPercentile(99.0),
            p999 = histogram.getValueAtPercentile(99.9),
            histogram = histogram
        )
    }
}
