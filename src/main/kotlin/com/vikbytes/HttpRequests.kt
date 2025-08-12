package com.vikbytes

import com.vikbytes.ResultFormatter.formatResults
import com.vikbytes.ResultFormatter.saveResultsToFile
import com.vikbytes.StatisticsHelper.calculateResponseTimeStats
import io.ktor.client.*
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpRedirect
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.*
import io.ktor.client.statement.bodyAsText
import io.ktor.http.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

object HttpRequests {

    fun createHttpClient(requestTimeout: Int, followRedirects: Boolean = false): HttpClient {
        return HttpClient(Java) {
            install(HttpTimeout) { requestTimeoutMillis = requestTimeout.toLong() }
            if (followRedirects) {
                install(HttpRedirect)
            }
            engine {
                protocolVersion = java.net.http.HttpClient.Version.HTTP_1_1
                dispatcher = Dispatchers.Default
            }
            defaultRequest { header(HttpHeaders.Connection, "close") }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun executeRequestsInParallel(
        client: HttpClient,
        url: String,
        method: String,
        headers: String?,
        authorization: String?,
        jsonBody: String?,
        concurrency: Int,
        totalRequests: Int?,
        durationSeconds: Int?,
        statistics: StatisticsHelper.RequestStatistics,
        progressTracker: AtomicInteger? = null,
        torture: Boolean = false,
        noBandwidth: Boolean = false
    ): Duration {
        executeRequest(
            client,
            url,
            method,
            headers,
            authorization,
            jsonBody,
            statistics.successCount,
            statistics.failureCount,
            statistics.responseTimes,
            statistics.statusCodes,
            statistics.requestBytes,
            statistics.responseBytes,
            progressTracker,
            true,
            noBandwidth,
            statistics.histogram)
        val startTime = System.currentTimeMillis()
        var dispatcher = Dispatchers.Default.limitedParallelism(concurrency)
        if (torture) {
            dispatcher = Dispatchers.Default
        }
        val scope = CoroutineScope(dispatcher)
        val shouldStop = AtomicBoolean(false)
        val requestCounter = AtomicInteger(0)
        val endTime =
            if (durationSeconds != null) {
                System.currentTimeMillis() + (durationSeconds * 1000L)
            } else {
                Long.MAX_VALUE
            }

        val jobs =
            List(concurrency) { threadIndex ->
                scope.launch {
                    if (totalRequests != null) {
                        while (!shouldStop.get()) {
                            if (requestCounter.incrementAndGet() > totalRequests) {
                                shouldStop.set(true)
                                break
                            }
                            executeRequest(
                                client,
                                url,
                                method,
                                headers,
                                authorization,
                                jsonBody,
                                statistics.successCount,
                                statistics.failureCount,
                                statistics.responseTimes,
                                statistics.statusCodes,
                                statistics.requestBytes,
                                statistics.responseBytes,
                                progressTracker,
                                false,
                                noBandwidth,
                                statistics.histogram)
                        }
                    } else if (durationSeconds != null) {
                        while (!shouldStop.get()) {
                            if (System.currentTimeMillis() >= endTime) {
                                shouldStop.set(true)
                                break
                            }
                            executeRequest(
                                client,
                                url,
                                method,
                                headers,
                                authorization,
                                jsonBody,
                                statistics.successCount,
                                statistics.failureCount,
                                statistics.responseTimes,
                                statistics.statusCodes,
                                statistics.requestBytes,
                                statistics.responseBytes,
                                progressTracker,
                                false,
                                noBandwidth,
                                statistics.histogram)
                        }
                    }
                }
            }

        jobs.joinAll()

        val finalTime = System.currentTimeMillis()
        return Duration.parse("${finalTime - startTime}ms")
    }

    suspend fun executeParallelRequests(
        url: String,
        method: String,
        headers: String?,
        authorization: String?,
        jsonBody: String?,
        concurrency: Int,
        totalRequests: Int?,
        durationSeconds: Int?,
        requestTimeout: Int,
        saveToFile: Boolean,
        headless: Boolean,
        torture: Boolean = false,
        noBandwidth: Boolean = false,
        followRedirects: Boolean = false
    ): String {
        val client = createHttpClient(requestTimeout, followRedirects)

        try {
            val statistics = StatisticsHelper.RequestStatistics()
            val progressTracker: AtomicInteger?
            if (!headless) {
                if (totalRequests != null) {
                    val message = "Executing requests"
                    val (job, progress) = LoadingAnimation.startProgressBar(message, totalRequests)
                    progressTracker = progress
                } else {
                    val message = "Executing requests for ${durationSeconds}s"
                    progressTracker = null
                    LoadingAnimation.startRequestSpinner(message, statistics.successCount, statistics.failureCount)
                }
            } else {
                progressTracker = null
            }

            executeRequestsInParallel(
                client,
                url,
                method,
                headers,
                authorization,
                jsonBody,
                concurrency,
                totalRequests,
                durationSeconds,
                statistics,
                progressTracker,
                torture,
                noBandwidth)

            if (!headless) {
                LoadingAnimation.stopAnimation()
                println()
            }

            val totalExecutionTime =
                if (statistics.responseTimes.isNotEmpty()) {
                    (statistics.responseTimes.sum() / concurrency)
                } else {
                    0L
                }

            val responseTimeStats = calculateResponseTimeStats(statistics.responseTimes)
            val results =
                formatResults(statistics, totalExecutionTime, concurrency, responseTimeStats, noBandwidth, torture)

            if (saveToFile) {
                saveResultsToFile(url, results)
            }

            if (headless) {
                if (statistics.failureCount.get() > 0) {
                    println("Failed requests: ".boldRed() + statistics.failureCount.get().toString().red())
                    exitProcess(1)
                } else {
                    println("All requests were successful".green())
                    exitProcess(0)
                }
            }

            print(results)

            return results
        } finally {
            client.close()
        }
    }

    suspend fun executeRequest(
        client: HttpClient,
        url: String,
        method: String,
        headers: String?,
        authorization: String?,
        jsonBody: String?,
        successCount: AtomicInteger,
        failureCount: AtomicInteger,
        responseTimes: MutableList<Long>,
        statusCodes: ConcurrentHashMap<Int, AtomicInteger>,
        requestBytes: AtomicInteger,
        responseBytes: AtomicInteger,
        progressTracker: AtomicInteger? = null,
        silent: Boolean = false,
        noBandwidth: Boolean = false,
        histogram: org.HdrHistogram.Histogram? = null
    ) {
        try {
            val startTime = System.currentTimeMillis()

            if (!noBandwidth) {
                var requestSize = 0

                // Request line (METHOD URL HTTP/1.1\r\n) - approximate size
                requestSize += method.length + url.length + 12

                // Add headers size estimation
                headers?.split(",")?.forEach { header ->
                    val parts = header.split(":", limit = 2)
                    if (parts.size == 2) {
                        // Header name + ": " + value + "\r\n"
                        requestSize += parts[0].trim().length + 2 + parts[1].trim().length + 2
                    }
                }

                // Add authorization header if present
                authorization?.let {
                    // "Authorization: " + value + "\r\n"
                    requestSize += 14 + it.length + 2
                }

                // Add Content-Type header for JSON
                jsonBody?.let {
                    // "Content-Type: application/json\r\n"
                    requestSize += 30
                }

                // Add Content-Length header
                jsonBody?.let {
                    val bodySize = it.toByteArray().size
                    // "Content-Length: " + bodySize digits + "\r\n"
                    requestSize += 16 + bodySize.toString().length + 2
                    // Add the actual body size
                    requestSize += bodySize
                }

                // Add final \r\n that separates headers from body
                requestSize += 2

                requestBytes.addAndGet(requestSize)
            }

            val response =
                client.request(url) {
                    this.method = HttpMethod.parse(method)

                    headers?.split(",")?.forEach { header ->
                        val parts = header.split(":", limit = 2)
                        if (parts.size == 2) {
                            header(parts[0].trim(), parts[1].trim())
                        }
                    }

                    authorization?.let { header(HttpHeaders.Authorization, it) }

                    jsonBody?.let {
                        contentType(ContentType.Application.Json)
                        setBody(it)
                    }
                }

            val responseBody = response.bodyAsText()

            if (!noBandwidth) {
                val responseBodySize = responseBody.toByteArray().size
                var responseSize = responseBodySize

                // Status line (HTTP/1.1 STATUS_CODE STATUS_TEXT\r\n) - approximate size
                responseSize += 9 + response.status.value.toString().length + response.status.description.length + 2

                // Add common response headers estimation (conservative estimate)
                // Common headers like Content-Type, Content-Length, Date, Server, etc.
                responseSize += 150
                responseBytes.addAndGet(responseSize)
                responseBytes.addAndGet(responseBody.toByteArray().size)
            }

            val endTime = System.currentTimeMillis()
            val responseTime = endTime - startTime
            if (!silent) {
                responseTimes.add(responseTime)
                histogram?.recordValue(responseTime)
                if (response.status.isSuccess()) {
                    successCount.incrementAndGet()
                } else {
                    failureCount.incrementAndGet()
                }
                progressTracker?.incrementAndGet()
                statusCodes.computeIfAbsent(response.status.value) { AtomicInteger(0) }.incrementAndGet()
            }
        } catch (e: Exception) {
            if (!silent) {
                failureCount.incrementAndGet()
                progressTracker?.incrementAndGet()
            }
        }
    }
}
