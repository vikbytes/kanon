package com.vikbytes

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.UsageError
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.vikbytes.CurlHelpers.processCurlCommand
import com.vikbytes.HttpRequests.executeParallelRequests
import java.io.File
import java.net.InetAddress
import java.net.URI
import java.util.Properties
import kotlinx.coroutines.*

class KanonCommand : CliktCommand(help = "A tool for load testing HTTP endpoints") {
    private val version: String by lazy {
        val properties = Properties()
        try {
            val inputStream = javaClass.classLoader.getResourceAsStream("version.properties")
            if (inputStream != null) {
                properties.load(inputStream)
                properties.getProperty("version", "unknown")
            } else {
                "unknown"
            }
        } catch (e: Exception) {
            println("Failed to load version information: ${e.message}")
            "unknown"
        }
    }

    private val showVersion by option("--version", "-v").flag().help("Show version information and exit")
    private val ci by
        option("--ci").flag().help("Runs in CI mode, returns exit code 0 if all requests succeeded, 1 otherwise.")
    private val url by option("-u", "--url").help("URL to send requests to")
    private val headers by option("-h", "--headers").help("HTTP headers to include in the request")
    private val authorization by option("-a", "--authorization").help("Authorization header value")
    private val jsonBody by option("-b", "--body").help("JSON body for the request")
    private val threads by option("-c", "--concurrent").int().default(1).help("Number of concurrent requests")
    private val totalRequests by option("-n", "--number").int().help("Total number of requests to make")
    private val durationSeconds by option("-d", "--duration").int().help("Duration in seconds to execute requests")
    private val method by
        option("-m", "--method").default("GET").help("HTTP method to use (GET, POST, PUT, DELETE, etc.)")
    private val requestTimeout by
        option("-t", "--timeout").int().default(10_000).help("Request timeout in milliseconds")
    private val saveToFile by option("-f", "--file").flag().help("Save results to a file")
    private val curlFile by option("--curl-file").help("Path to a file containing a cURL command to parse")
    private val curl by option("--curl").help("cURL command to parse directly (include the 'curl' prefix)")
    private val torture by
        option("--torture").flag().help("Executes as many threads as possible, ignoring the concurrency option")
    private val noBandwidth by option("--no-bandwidth").flag().help("Disables bandwidth collection and reporting")
    private val followRedirects by option("-l", "--location").flag().help("Follow redirects (HTTP 301, 302, etc.)")

    fun validateArguments() {
        if ((totalRequests == null && durationSeconds == null) || (totalRequests != null && durationSeconds != null)) {
            throw UsageError(
                "You must provide either ".bold() +
                    "--number".boldYellow() +
                    " or ".bold() +
                    "--duration".boldYellow() +
                    ", but not both".bold())
        }

        if (curl != null && curlFile != null) {
            throw UsageError(
                "You must provide either ".bold() +
                    "--curl".boldYellow() +
                    " or ".bold() +
                    "--curl-file".boldYellow() +
                    ", but not both".bold())
        }

        if ((curl != null || curlFile != null) &&
            (url != null || headers != null || authorization != null || jsonBody != null || method != "GET")) {
            throw UsageError(
                "When using ".bold() +
                    "--curl".boldYellow() +
                    " or ".bold() +
                    "--curl-file".boldYellow() +
                    ", you cannot use ".bold() +
                    "--url".boldRed() +
                    ", ".bold() +
                    "--headers".boldRed() +
                    ", ".bold() +
                    "--authorization".boldRed() +
                    ", ".bold() +
                    "--body".boldRed() +
                    ", ".bold() +
                    "--method".boldRed())
        }
    }

    fun validateUrl(url: String) {
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "http://$url"
                .let {
                    try {
                        InetAddress.getByName(URI(it).host)
                        url.replace("http://", "https://")
                    } catch (e: Exception) {
                        println("Failed to resolve DNS for $url: ${e.message}".boldRed())
                        throw UsageError("Failed to resolve DNS for $url: $it".boldRed())
                    }
                }
        }
    }

    override fun run() {
        if (showVersion) {
            println("Kanon version $version")
            return
        }

        validateArguments()

        val requestParams = getRequestParameters()
        validateUrl(requestParams["url"]!!)

        displayRequestInfo(requestParams)

        runBlocking {
            executeParallelRequests(
                url = requestParams["url"]!!,
                method = requestParams["method"]!!,
                headers = requestParams["headers"],
                authorization = requestParams["authorization"],
                jsonBody = requestParams["body"],
                concurrency = threads,
                totalRequests = totalRequests,
                durationSeconds = durationSeconds,
                requestTimeout = requestTimeout,
                saveToFile = saveToFile,
                headless = ci,
                torture = torture,
                noBandwidth = noBandwidth,
                followRedirects = requestParams["followRedirects"]?.toBoolean() ?: followRedirects)
        }
    }

    fun getRequestParameters(): Map<String, String?> {
        var requestUrl = url
        var requestMethod = method
        var requestHeaders = headers
        var requestAuthorization = authorization
        var requestJsonBody = jsonBody
        var requestFollowRedirects = followRedirects.toString()

        if (curl != null) {
            val curlString = curl.toString()
            val curlParams = processCurlCommand(curlString, false, curlString)
            requestUrl = curlParams["url"]
            requestMethod = curlParams["method"] ?: method
            requestHeaders = curlParams["headers"]
            requestAuthorization = curlParams["authorization"]
            requestJsonBody = curlParams["body"]
            requestFollowRedirects = curlParams["followRedirects"] ?: followRedirects.toString()
        } else if (curlFile != null && requestUrl == null) {
            val curlContent = File(curlFile!!).readText()
            val curlParams = processCurlCommand(curlContent, true, curlFile!!)
            requestUrl = curlParams["url"]
            requestMethod = curlParams["method"] ?: method
            requestHeaders = curlParams["headers"]
            requestAuthorization = curlParams["authorization"]
            requestJsonBody = curlParams["body"]
            requestFollowRedirects = curlParams["followRedirects"] ?: followRedirects.toString()
        } else if (requestUrl == null) {
            throw UsageError(
                "URL is required. ".boldRed() +
                    "Provide ".bold() +
                    "--url".boldYellow() +
                    ", ".bold() +
                    "--curl-file".boldYellow() +
                    ", or ".bold() +
                    "--curl".boldYellow())
        }

        return mapOf(
            "url" to requestUrl,
            "method" to requestMethod,
            "headers" to requestHeaders,
            "authorization" to requestAuthorization,
            "body" to requestJsonBody,
            "followRedirects" to requestFollowRedirects)
    }

    fun displayRequestInfo(requestParams: Map<String, String?>) {
        val requestUrl = requestParams["url"]
        val requestMethod = requestParams["method"]
        val requestHeaders = requestParams["headers"]
        val requestAuthorization = requestParams["authorization"]
        val requestJsonBody = requestParams["body"]
        val requestFollowRedirects = requestParams["followRedirects"]?.toBoolean() ?: followRedirects

        val argsToPrint = mutableListOf<String>()
        print("Kanon".boldPurple() + " ")
        println("v$version".boldGreen())

        if (totalRequests != null) {
            println(
                "Running ".bold() +
                    requestMethod!!.boldYellow() +
                    " requests to ".bold() +
                    requestUrl!!.boldBlue() +
                    (if (!torture) {
                        " with ".bold() + threads.toString().boldGreen() + " threads, ".bold()
                    } else {
                        " ".bold()
                    }) +
                    totalRequests.toString().boldGreen() +
                    " total requests, and ".bold() +
                    "${requestTimeout}ms".boldYellow() +
                    " timeout...".bold())
        } else {
            println(
                "Running ".bold() +
                    requestMethod!!.boldYellow() +
                    " requests to ".bold() +
                    requestUrl!!.boldBlue() +
                    (if (!torture) {
                        " with ".bold() + threads.toString().boldGreen() + " threads for ".bold()
                    } else {
                        " for ".bold()
                    }) +
                    durationSeconds.toString().boldGreen() +
                    " seconds, and ".bold() +
                    "${requestTimeout}ms".boldYellow() +
                    " timeout...".bold())
        }

        if (requestHeaders != null) {
            argsToPrint.add("Headers: ".bold() + requestHeaders.cyan())
        }

        if (requestAuthorization != null) {
            argsToPrint.add("Authorization: ".bold() + requestAuthorization.cyan())
        }

        if (requestJsonBody != null) {
            argsToPrint.add("JSON Body: ".bold() + requestJsonBody.cyan())
        }

        if (requestFollowRedirects) {
            argsToPrint.add("Following redirects: ".bold() + "enabled".green())
        }

        if (argsToPrint.isNotEmpty()) {
            argsToPrint.forEach { println(it) }
        }
    }
}

fun main(args: Array<String>) = KanonCommand().main(args)
