package com.github.michael72.pumlsrv

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

/**
 * Cache for remote !include URLs.
 * Downloads the content once and returns it on subsequent requests.
 * The content is stripped of @startuml/@enduml markers so it can be
 * inlined directly into the calling UML source.
 *
 * Uses a ConcurrentHashMap with a configurable max size to prevent unbounded growth.
 * When the cache exceeds the max size, it is cleared entirely (simple eviction strategy).
 */
object RemoteIncludeCache {
    private const val MAX_CACHE_SIZE = 200

    /** Maximum size of a single fetched include (1 MiB). */
    private const val MAX_INCLUDE_BYTES = 1024L * 1024L

    /** Maximum number of redirects followed while fetching an include. */
    private const val MAX_REDIRECTS = 3

    private val cache = ConcurrentHashMap<String, String>()

    private val INCLUDE_PATTERN = Regex("""^!include\s+(https?://\S+)\s*$""")

    /**
     * Resolve all remote !include directives in the given UML source.
     * Lines matching `!include https://...` or `!include http://...` are replaced
     * with the cached (or freshly downloaded) content, stripped of @startuml/@enduml.
     */
    fun resolveRemoteIncludes(uml: String): String {
        val lines = uml.lines()
        val result = StringBuilder()

        for ((index, line) in lines.withIndex()) {
            val trimmed = line.trim()
            val url = extractRemoteIncludeUrl(trimmed)
            if (url != null) {
                val content = getOrFetch(url)
                if (content != null) {
                    result.append(content)
                } else {
                    // Download failed — keep original line so PlantUML can try or report the error
                    result.append(line)
                }
            } else {
                result.append(line)
            }
            if (index < lines.size - 1) {
                result.append('\n')
            }
        }

        return result.toString()
    }

    /**
     * Extract the URL from a remote !include directive, or null if the line is not one.
     * Supports: !include https://... and !include http://...
     * Does NOT match local file includes.
     */
    private fun extractRemoteIncludeUrl(line: String): String? {
        val match = INCLUDE_PATTERN.matchEntire(line) ?: return null
        return match.groupValues[1]
    }

    /**
     * Get cached content or fetch from the remote URL.
     */
    private fun getOrFetch(url: String): String? {
        cache[url]?.let { return it }

        return try {
            val raw = fetchSafely(url) ?: return null
            val stripped = stripStartEndUml(raw)

            // Simple eviction: clear entire cache if it grows too large
            if (cache.size >= MAX_CACHE_SIZE) {
                cache.clear()
            }
            cache[url] = stripped
            println("Cached remote include: $url (${stripped.length} chars)")
            stripped
        } catch (e: IOException) {
            System.err.println("Failed to fetch remote include: $url — ${e.message}")
            null
        } catch (t: Throwable) {
            System.err.println("Error fetching remote include: $url — ${t.message}")
            null
        }
    }

    /**
     * Fetch a remote include with SSRF protection.
     *
     * Every URL — including each redirect target — is validated with [RemoteIncludeGuard]
     * before a connection is made, so an attacker cannot reach internal resources directly
     * or via a redirect from a public host. Redirects are followed manually (with a bounded
     * count) instead of automatically so each hop can be re-validated, and the response body
     * is size-limited.
     */
    private fun fetchSafely(initialUrl: String): String? {
        var current = initialUrl

        for (hop in 0..MAX_REDIRECTS) {
            if (!RemoteIncludeGuard.isAllowed(current)) {
                System.err.println("Blocked remote include (SSRF protection): $current")
                return null
            }

            val connection = ConnectionHelper.getConnection(URL(current))
            connection.instanceFollowRedirects = false
            try {
                when (val code = connection.responseCode) {
                    in 300..399 -> {
                        val location = connection.getHeaderField("Location") ?: return null
                        // Resolve relative redirects against the current URL.
                        current = URL(URL(current), location).toString()
                    }
                    HttpURLConnection.HTTP_OK -> {
                        return connection.inputStream.use {
                            Download.getContent(it, MAX_INCLUDE_BYTES)
                        }
                    }
                    else -> {
                        System.err.println("Remote include returned HTTP $code: $current")
                        return null
                    }
                }
            } finally {
                connection.disconnect()
            }
        }

        System.err.println("Too many redirects for remote include: $initialUrl")
        return null
    }

    /**
     * Strip @startuml / @enduml markers from the downloaded content.
     */
    private fun stripStartEndUml(content: String): String {
        val lines = content.lines().toMutableList()

        // Remove leading @startuml (with optional diagram name)
        if (lines.isNotEmpty() && lines.first().trim().startsWith("@startuml")) {
            lines.removeFirst()
        }
        // Remove trailing @enduml
        if (lines.isNotEmpty() && lines.last().trim().startsWith("@enduml")) {
            lines.removeLast()
        }

        return lines.joinToString("\n")
    }

    /** Clear the cache (e.g. for testing or manual refresh). */
    fun clear() {
        cache.clear()
    }

    /** Current cache size. */
    fun size(): Int = cache.size
}
