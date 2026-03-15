package com.vikbytes

import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.HdrHistogram.Histogram
import org.junit.jupiter.api.Test

class StatisticsHelperTest {

    private fun histogramOf(vararg values: Long): Histogram {
        val histogram = Histogram(3600000L, 2)
        values.forEach { histogram.recordValue(it) }
        return histogram
    }

    @Test
    fun `RequestStatistics should handle concurrent updates correctly`() {
        val stats = StatisticsHelper.RequestStatistics()

        stats.successCount.incrementAndGet()
        stats.failureCount.incrementAndGet()
        stats.requestBytes.addAndGet(1024L)
        stats.responseBytes.addAndGet(2048L)

        assertEquals(1, stats.successCount.get())
        assertEquals(1, stats.failureCount.get())
        assertEquals(1024L, stats.requestBytes.get())
        assertEquals(2048L, stats.responseBytes.get())
    }

    @Test
    fun `RequestStatistics statusCodes should handle concurrent additions`() {
        val stats = StatisticsHelper.RequestStatistics()

        stats.statusCodes.computeIfAbsent(200) { AtomicInteger(0) }.incrementAndGet()
        stats.statusCodes.computeIfAbsent(404) { AtomicInteger(0) }.incrementAndGet()
        stats.statusCodes.computeIfAbsent(200) { AtomicInteger(0) }.incrementAndGet()

        assertEquals(2, stats.statusCodes[200]?.get())
        assertEquals(1, stats.statusCodes[404]?.get())
        assertEquals(2, stats.statusCodes.size)
    }

    @Test
    fun `RequestStatistics responseTimes should be thread-safe`() {
        val stats = StatisticsHelper.RequestStatistics()

        stats.responseTimes.add(100L)
        stats.responseTimes.add(200L)
        stats.responseTimes.add(150L)

        assertEquals(3, stats.responseTimes.size)
        assertTrue(stats.responseTimes.contains(100L))
        assertTrue(stats.responseTimes.contains(200L))
        assertTrue(stats.responseTimes.contains(150L))
    }

    @Test
    fun `calculateResponseTimeStats should handle identical values correctly`() {
        val histogram = histogramOf(100L, 100L, 100L, 100L, 100L)
        val stats = StatisticsHelper.calculateResponseTimeStats(histogram)

        assertEquals(100, stats.min)
        assertEquals(100, stats.max)
        assertEquals(100, stats.p25)
        assertEquals(100, stats.p50)
        assertEquals(100, stats.p75)
        assertEquals(100, stats.p90)
        assertEquals(100, stats.p95)
        assertEquals(100, stats.p99)
        assertEquals(100, stats.p999)
    }

    @Test
    fun `calculateResponseTimeStats should handle three values correctly`() {
        val histogram = histogramOf(10L, 20L, 30L)
        val stats = StatisticsHelper.calculateResponseTimeStats(histogram)

        assertEquals(10, stats.min)
        assertEquals(30, stats.max)
        assertTrue(stats.p25 >= 10)
        assertTrue(stats.p50 >= 10)
        assertTrue(stats.p75 >= 10)
        assertEquals(3, stats.histogram.totalCount)
    }

    @Test
    fun `test calculateResponseTimeStats with empty histogram`() {
        val histogram = Histogram(3600000L, 2)
        val stats = StatisticsHelper.calculateResponseTimeStats(histogram)

        assertEquals(0, stats.min)
        assertEquals(0, stats.max)
        assertEquals(0.0, stats.avg)
        assertEquals(0.0, stats.median)
        assertEquals(0, stats.p50)
        assertEquals(0, stats.p75)
        assertEquals(0, stats.p90)
        assertEquals(0, stats.p95)
        assertEquals(0, stats.p99)
        assertEquals(0, stats.p999)
        assertEquals(0, stats.histogram.totalCount)
    }

    @Test
    fun `test calculateResponseTimeStats with single value`() {
        val histogram = histogramOf(100L)
        val stats = StatisticsHelper.calculateResponseTimeStats(histogram)

        assertEquals(100, stats.min)
        assertEquals(100, stats.max)
        assertEquals(100, stats.p50)
        assertEquals(100, stats.p75)
        assertEquals(100, stats.p90)
        assertEquals(100, stats.p95)
        assertEquals(100, stats.p99)
        assertEquals(100, stats.p999)
        assertEquals(1, stats.histogram.totalCount)
    }

    @Test
    fun `test calculateResponseTimeStats with multiple values`() {
        val histogram = histogramOf(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L)
        val stats = StatisticsHelper.calculateResponseTimeStats(histogram)

        assertEquals(10, stats.min)
        assertEquals(100, stats.max)
        assertTrue(stats.p50 in 40..60)
        assertTrue(stats.p75 in 70..80)
        assertTrue(stats.p90 in 90..100)
        assertEquals(10, stats.histogram.totalCount)
    }

    @Test
    fun `test calculateResponseTimeStats with even number of values`() {
        val histogram = histogramOf(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L)
        val stats = StatisticsHelper.calculateResponseTimeStats(histogram)

        assertEquals(10, stats.min)
        assertEquals(80, stats.max)
        assertTrue(stats.p50 in 40..50)
        assertTrue(stats.p75 in 60..70)
        assertTrue(stats.p90 in 70..80)
        assertEquals(8, stats.histogram.totalCount)
    }

    @Test
    fun `test calculateResponseTimeStats with odd number of values`() {
        val histogram = histogramOf(10L, 20L, 30L, 40L, 50L, 60L, 70L)
        val stats = StatisticsHelper.calculateResponseTimeStats(histogram)

        assertEquals(10, stats.min)
        assertEquals(70, stats.max)
        assertTrue(stats.p50 in 30..40)
        assertTrue(stats.p75 in 50..60)
        assertTrue(stats.p90 in 60..70)
        assertEquals(7, stats.histogram.totalCount)
    }

    @Test
    fun `test calculateResponseTimeStats with large number of values`() {
        val histogram = Histogram(3600000L, 2)
        for (i in 1..1000) {
            histogram.recordValue(i.toLong())
        }
        val stats = StatisticsHelper.calculateResponseTimeStats(histogram)

        assertEquals(1, stats.min)
        assertTrue(stats.max in 999..1010, "max was ${stats.max}")
        assertTrue(stats.p50 in 480..520, "p50 was ${stats.p50}")
        assertTrue(stats.p75 in 730..770, "p75 was ${stats.p75}")
        assertTrue(stats.p90 in 880..920, "p90 was ${stats.p90}")
        assertTrue(stats.p95 in 930..970, "p95 was ${stats.p95}")
        assertTrue(stats.p99 in 980..1010, "p99 was ${stats.p99}")
        assertEquals(1000, stats.histogram.totalCount)
    }

    @Test
    fun `test RequestStatistics initialization`() {
        val stats = StatisticsHelper.RequestStatistics()

        assertEquals(0, stats.successCount.get())
        assertEquals(0, stats.failureCount.get())
        assertTrue(stats.responseTimes.isEmpty())
        assertTrue(stats.statusCodes.isEmpty())
        assertEquals(0L, stats.requestBytes.get())
        assertEquals(0L, stats.responseBytes.get())
        assertEquals(0, stats.histogram.totalCount)
    }

    @Test
    fun `calculateResponseTimeStats avg is much higher than median for skewed distribution`() {
        val histogram = Histogram(3600000L, 2)
        repeat(99) { histogram.recordValue(1L) }
        histogram.recordValue(10000L)

        val stats = StatisticsHelper.calculateResponseTimeStats(histogram)

        // avg should be ~100, median should be 1
        assertTrue(stats.avg > 90, "avg (${stats.avg}) should be much higher than median for skewed data")
        assertEquals(1.0, stats.median, "median should be 1.0 for skewed distribution")
        assertEquals(1, stats.min)
        assertTrue(stats.max in 9990..10100, "max was ${stats.max}")
    }

    @Test
    fun `ConcurrentLinkedQueue responseTimes handles concurrent writes`() {
        val stats = StatisticsHelper.RequestStatistics()
        val threads = (1..100).map { i ->
            Thread { stats.responseTimes.add(i.toLong()) }
        }
        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(100, stats.responseTimes.size, "All 100 concurrent additions should be reflected")
    }

    @Test
    fun `AtomicLong requestBytes handles values above Int MAX_VALUE`() {
        val stats = StatisticsHelper.RequestStatistics()
        val largeValue = Int.MAX_VALUE.toLong() + 1000L
        stats.requestBytes.addAndGet(largeValue)

        assertEquals(largeValue, stats.requestBytes.get(), "AtomicLong should handle values above Int.MAX_VALUE")
        assertTrue(stats.requestBytes.get() > Int.MAX_VALUE, "Value should exceed Int.MAX_VALUE")
    }

    @Test
    fun `histogram-based stats match percentile order`() {
        val histogram = Histogram(3600000L, 2)
        // Record values 1 through 100
        for (i in 1..100) histogram.recordValue(i.toLong())

        val stats = StatisticsHelper.calculateResponseTimeStats(histogram)

        assertTrue(stats.min <= stats.p25, "min <= p25")
        assertTrue(stats.p25 <= stats.p50, "p25 <= p50")
        assertTrue(stats.p50 <= stats.p75, "p50 <= p75")
        assertTrue(stats.p75 <= stats.p90, "p75 <= p90")
        assertTrue(stats.p90 <= stats.p95, "p90 <= p95")
        assertTrue(stats.p95 <= stats.p99, "p95 <= p99")
        assertTrue(stats.p99 <= stats.p999, "p99 <= p999")
        assertTrue(stats.p999 <= stats.max, "p999 <= max")
    }
}
