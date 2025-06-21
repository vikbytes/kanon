package com.vikbytes

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StatisticsHelperTest {

    @Test
    fun `test calculateResponseTimeStats with empty list`() {
        val responseTimes = emptyList<Long>()
        val stats = StatisticsHelper.calculateResponseTimeStats(responseTimes)

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
        val responseTimes = listOf(100L)
        val stats = StatisticsHelper.calculateResponseTimeStats(responseTimes)

        assertEquals(100, stats.min)
        assertEquals(100, stats.max)
        assertEquals(100.0, stats.avg)
        assertEquals(100.0, stats.median)
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
        val responseTimes = listOf(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L, 90L, 100L)
        val stats = StatisticsHelper.calculateResponseTimeStats(responseTimes)

        assertEquals(10, stats.min)
        assertEquals(100, stats.max)
        assertEquals(55.0, stats.avg)
        assertEquals(55.0, stats.median)
        assertEquals(60, stats.p50)  // Index 5 (50% of 10 items)
        assertEquals(80, stats.p75)
        assertEquals(100, stats.p90)  // Index 9 (90% of 10 items)
        assertEquals(100, stats.p95)
        assertEquals(100, stats.p99)
        assertEquals(100, stats.p999)
        assertEquals(10, stats.histogram.totalCount)
    }

    @Test
    fun `test calculateResponseTimeStats with even number of values`() {
        val responseTimes = listOf(10L, 20L, 30L, 40L, 50L, 60L, 70L, 80L)
        val stats = StatisticsHelper.calculateResponseTimeStats(responseTimes)

        assertEquals(10, stats.min)
        assertEquals(80, stats.max)
        assertEquals(45.0, stats.avg)
        assertEquals(45.0, stats.median) // (40 + 50) / 2 = 45
        assertEquals(50, stats.p50)  // Index 4 (50% of 8 items)
        assertEquals(70, stats.p75)
        assertEquals(80, stats.p90)
        assertEquals(80, stats.p95)
        assertEquals(80, stats.p99)
        assertEquals(80, stats.p999)
        assertEquals(8, stats.histogram.totalCount)
    }

    @Test
    fun `test calculateResponseTimeStats with odd number of values`() {
        val responseTimes = listOf(10L, 20L, 30L, 40L, 50L, 60L, 70L)
        val stats = StatisticsHelper.calculateResponseTimeStats(responseTimes)

        assertEquals(10, stats.min)
        assertEquals(70, stats.max)
        assertEquals(40.0, stats.avg)
        assertEquals(40.0, stats.median) // Middle value is 40
        assertEquals(40, stats.p50)
        assertEquals(60, stats.p75)
        assertEquals(70, stats.p90)
        assertEquals(70, stats.p95)
        assertEquals(70, stats.p99)
        assertEquals(70, stats.p999)
        assertEquals(7, stats.histogram.totalCount)
    }

    @Test
    fun `test calculateResponseTimeStats with large number of values`() {
        // Create a list of 1000 response times from 1 to 1000
        val responseTimes = (1..1000).map { it.toLong() }
        val stats = StatisticsHelper.calculateResponseTimeStats(responseTimes)

        assertEquals(1, stats.min)
        assertEquals(1000, stats.max)
        assertEquals(500.5, stats.avg)
        assertEquals(500.5, stats.median)
        assertEquals(501, stats.p50)  // Index 500 (50% of 1000 items)
        assertEquals(751, stats.p75)  // Index 750 (75% of 1000 items)
        assertEquals(901, stats.p90)  // Index 900 (90% of 1000 items)
        assertEquals(951, stats.p95)  // Index 950 (95% of 1000 items)
        assertEquals(991, stats.p99)  // Index 990 (99% of 1000 items)
        assertEquals(1000, stats.p999)  // Index 999 (99.9% of 1000 items)
        assertEquals(1000, stats.histogram.totalCount)
    }

    @Test
    fun `test RequestStatistics initialization`() {
        val stats = StatisticsHelper.RequestStatistics()

        assertEquals(0, stats.successCount.get())
        assertEquals(0, stats.failureCount.get())
        assertTrue(stats.responseTimes.isEmpty())
        assertTrue(stats.statusCodes.isEmpty())
        assertEquals(0, stats.requestBytes.get())
        assertEquals(0, stats.responseBytes.get())
        assertEquals(0, stats.histogram.totalCount)
    }
}
