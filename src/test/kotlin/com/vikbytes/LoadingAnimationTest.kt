package com.vikbytes

import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

class LoadingAnimationTest {

    @AfterEach
    fun cleanup() {
        LoadingAnimation.stopAnimation()
    }

    @Test
    fun `test startProgressBar returns valid job and progress`() {
        val (job, progress) = LoadingAnimation.startProgressBar("Testing", 100)

        try {
            assertTrue(job.isActive)

            assertEquals(0, progress.get())

            progress.set(50)
            assertEquals(50, progress.get())
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `test startRequestSpinner returns valid job`() {
        val successCount = AtomicInteger(0)
        val failureCount = AtomicInteger(0)

        val job = LoadingAnimation.startRequestSpinner("Testing", successCount, failureCount)

        try {
            assertTrue(job.isActive)

            successCount.incrementAndGet()
            failureCount.incrementAndGet()
            assertEquals(1, successCount.get())
            assertEquals(1, failureCount.get())
        } finally {
            job.cancel()
        }
    }

    @Test
    fun `test stopAnimation cancels job`() = runBlocking {
        val (job, _) = LoadingAnimation.startProgressBar("Testing", 100)

        assertTrue(job.isActive)

        LoadingAnimation.stopAnimation()

        delay(100)

        assertTrue(job.isCancelled)
    }

    @Test
    fun `test starting new animation stops previous one`() = runBlocking {
        val (job1, _) = LoadingAnimation.startProgressBar("Testing 1", 100)

        assertTrue(job1.isActive)

        val (job2, _) = LoadingAnimation.startProgressBar("Testing 2", 100)

        delay(100)

        assertTrue(job1.isCancelled)
        assertTrue(job2.isActive)

        job2.cancel()
    }

    @Test
    fun `test progress bar completes when progress reaches total`() = runBlocking {
        val total = 10
        val (job, progress) = LoadingAnimation.startProgressBar("Testing", total)

        progress.set(total)

        withTimeout(1000) { job.join() }

        assertTrue(job.isCompleted)
    }

    @Test
    fun `test different animation types can be started and stopped`() {
        val (progressJob, _) = LoadingAnimation.startProgressBar("Testing Progress", 100)
        assertTrue(progressJob.isActive)

        val spinnerJob = LoadingAnimation.startRequestSpinner("Testing Spinner", AtomicInteger(0), AtomicInteger(0))

        assertTrue(spinnerJob.isActive)

        LoadingAnimation.stopAnimation()
    }
}
