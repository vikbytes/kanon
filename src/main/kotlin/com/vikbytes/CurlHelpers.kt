package com.vikbytes

import com.github.ajalt.clikt.core.UsageError
import java.util.Base64
import java.util.regex.Pattern

object CurlHelpers {

    data class CurlCommand(
        val url: String,
        val method: String = "GET",
        val headers: MutableMap<String, String> = mutableMapOf(),
        val body: String? = null,
        val followRedirects: Boolean = false
    )

    fun processCurlCommand(
        curlString: String,
        isFromFile: Boolean = false,
        curlFile: String = ""
    ): Map<String, String?> {
        try {
            val normalizedCurlString =
                if (!curlString.trim().startsWith("curl ")) {
                    "curl $curlString"
                } else {
                    curlString
                }
            val curlCommand = parseCurlCommand(normalizedCurlString)
            val result = mutableMapOf<String, String?>()

            result["url"] = curlCommand.url
            result["method"] = curlCommand.method

            if (curlCommand.headers.isNotEmpty()) {
                val headersList = curlCommand.headers.map { "${it.key}: ${it.value}" }
                result["headers"] = headersList.joinToString(",")

                val authHeader = curlCommand.headers["Authorization"] ?: curlCommand.headers["authorization"]
                if (authHeader != null) {
                    result["authorization"] = authHeader
                }
            }

            result["body"] = curlCommand.body
            result["followRedirects"] = curlCommand.followRedirects.toString()

            if (isFromFile) {
                println("Parsed cURL command from file: ".green() + curlFile.boldGreen())
            } else {
                println("Parsed cURL command from command line".green())
            }

            return result
        } catch (e: Exception) {
            val errorMessage =
                if (isFromFile) {
                    "Failed to parse cURL command from file: ".boldRed() + "${e.message}".red()
                } else {
                    "Failed to parse cURL command: ".boldRed() + "${e.message}".red()
                }
            throw UsageError(errorMessage)
        }
    }

    fun parseCurlCommand(curlString: String): CurlCommand {
        val normalizedCurlString =
            if (!curlString.trim().startsWith("curl ")) {
                "curl $curlString"
            } else {
                curlString
            }
        val trimmedCommand =
            normalizedCurlString.trim().let { if (it.startsWith("curl ")) it.substring(5).trim() else it }

        var url = ""
        var method = "GET"
        val headers = mutableMapOf<String, String>()
        var body: String? = null
        val bodyParts = mutableListOf<String>()
        var userAgent: String?
        var referer: String?
        var cookie: String?
        var basicAuth: String?
        var followRedirects = false

        val parts = splitRespectingQuotes(trimmedCommand)

        var i = 0
        while (i < parts.size) {
            val part = parts[i]
            when {
                part == "-X" || part == "--request" -> {
                    if (i + 1 < parts.size) {
                        method = parts[++i].uppercase()
                    }
                }

                part == "-H" || part == "--header" -> {
                    if (i + 1 < parts.size) {
                        val header = parts[++i].trim('"', '\'')
                        val colonIndex = header.indexOf(':')
                        if (colonIndex > 0) {
                            val key = header.take(colonIndex).trim()
                            val value = header.substring(colonIndex + 1).trim()
                            headers[key] = value
                        }
                    }
                }

                part == "-d" || part == "--data" || part == "--data-ascii" || part == "--data-binary" -> {
                    if (i + 1 < parts.size) {
                        bodyParts.add(parts[++i].trim('"', '\''))
                        if (method == "GET") {
                            method = "POST"
                        }
                    }
                }

                part.startsWith("-d") -> {
                    bodyParts.add(part.substring(2).trim('"', '\''))
                    if (method == "GET") {
                        method = "POST"
                    }
                }

                part == "--data-urlencode" -> {
                    if (i + 1 < parts.size) {
                        bodyParts.add(parts[++i].trim('"', '\''))
                        if (method == "GET") {
                            method = "POST"
                        }
                    }
                }

                part == "-A" || part == "--user-agent" -> {
                    if (i + 1 < parts.size) {
                        userAgent = parts[++i].trim('"', '\'')
                        headers["User-Agent"] = userAgent
                    }
                }

                part == "-e" || part == "--referer" -> {
                    if (i + 1 < parts.size) {
                        referer = parts[++i].trim('"', '\'')
                        headers["Referer"] = referer
                    }
                }

                part == "-b" || part == "--cookie" -> {
                    if (i + 1 < parts.size) {
                        cookie = parts[++i].trim('"', '\'')
                        headers["Cookie"] = cookie
                    }
                }

                part == "-u" || part == "--user" -> {
                    if (i + 1 < parts.size) {
                        basicAuth = parts[++i].trim('"', '\'')
                        val encodedAuth = Base64.getEncoder().encodeToString(basicAuth.toByteArray())
                        headers["Authorization"] = "Basic $encodedAuth"
                    }
                }

                part == "--url" -> {
                    if (i + 1 < parts.size) {
                        url = parts[++i].trim('"', '\'')
                    }
                }

                part == "-o" || part == "--output" -> {
                    // Skip output file parameter
                    if (i + 1 < parts.size) {
                        i++
                    }
                }

                part == "-L" || part == "--location" -> {
                    followRedirects = true
                }

                part == "-k" || part == "--insecure" -> {
                    // Flag for insecure connections, no action needed for parsing
                }

                !part.startsWith("-") && url.isEmpty() -> {
                    url = part.trim('"', '\'')
                }
            }

            i++
        }

        if (url.isEmpty()) {
            throw IllegalArgumentException("URL not found in cURL command")
        }

        if (bodyParts.isNotEmpty()) {
            body = bodyParts.joinToString("&")
        }

        return CurlCommand(url, method, headers, body, followRedirects)
    }

    fun splitRespectingQuotes(input: String): List<String> {
        val result = mutableListOf<String>()
        // 1. Sequences of non-whitespace, non-quote characters
        // 2. Double-quoted strings, handling escaped quotes
        // 3. Single-quoted strings, handling escaped quotes
        val pattern = Pattern.compile("[^\\s\"']+|\"((?:\\\\.|[^\\\\\"])*)\"|'((?:\\\\.|[^\\\\'])*)'")
        val matcher = pattern.matcher(input)

        while (matcher.find()) {
            if (matcher.group(1) != null) {
                val content = matcher.group(1).replace("\\\\", "\\").replace("\\\"", "\"")
                result.add("\"" + content + "\"")
            } else if (matcher.group(2) != null) {
                val content = matcher.group(2).replace("\\\\", "\\").replace("\\'", "'")
                result.add("'$content'")
            } else {
                result.add(matcher.group())
            }
        }

        return result
    }
}
