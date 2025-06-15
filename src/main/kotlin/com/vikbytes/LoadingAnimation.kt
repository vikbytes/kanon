package com.vikbytes

import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.*

object LoadingAnimation {
    private val isRunning = AtomicBoolean(false)
    private var animationJob: Job? = null

    private val spinnerFrames = listOf("⠋", "⠙", "⠹", "⠸", "⠼", "⠴", "⠦", "⠧", "⠇", "⠏")

    fun startProgressBar(
        message: String,
        total: Int,
        color: (String) -> String = { it.green() }
    ): Pair<Job, AtomicInteger> {
        if (isRunning.getAndSet(true)) {
            stopAnimation()
        }

        val progress = AtomicInteger(0)

        animationJob =
            CoroutineScope(Dispatchers.IO).launch {
                while (isActive) {
                    val current = progress.get()
                    val percentage = if (total > 0) (current.toDouble() / total * 100).toInt() else 0
                    val progressBar = createProgressBar(percentage)

                    print("\r$message ${color(progressBar)} $percentage% ($current/$total)")

                    if (current >= total) {
                        break
                    }

                    delay(100) // Update every 100ms
                }
            }

        return Pair(animationJob!!, progress)
    }

    fun startRequestSpinner(message: String, successCount: AtomicInteger, failureCount: AtomicInteger): Job {
        if (isRunning.getAndSet(true)) {
            stopAnimation()
        }

        animationJob =
            CoroutineScope(Dispatchers.IO).launch {
                var frameIndex = 0
                while (isActive) {
                    val frame = spinnerFrames[frameIndex]
                    val successText = "${successCount.get()}".green()
                    val failureText =
                        if (failureCount.get() > 0) "${failureCount.get()}".red() else "${failureCount.get()}"

                    print("\r${frame.cyan()} $message Success: $successText Failure: $failureText")
                    frameIndex = (frameIndex + 1) % spinnerFrames.size
                    delay(80)
                }
            }

        return animationJob!!
    }

    fun stopAnimation() {
        animationJob?.cancel()
        animationJob = null
        isRunning.set(false)
        print("\r" + " ".repeat(100) + "\r")
    }

    private fun createProgressBar(percentage: Int): String {
        val width = 20
        val completed = (width * percentage / 100)
        val remaining = width - completed

        return "[" + "=".repeat(completed) + " ".repeat(remaining) + "]"
    }
}
