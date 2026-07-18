package com.github.michael72.pumlsrv

import java.net.InetAddress
import java.net.URL

/**
 * Guards remote `!include` fetching against Server-Side Request Forgery (SSRF).
 *
 * User-supplied UML source may contain `!include https://host/...` directives whose
 * target is fetched by the server (see [RemoteIncludeCache]). Without validation an
 * attacker could point these at internal-only resources — cloud metadata endpoints
 * (e.g. 169.254.169.254), localhost admin ports or private network hosts — and read
 * the response back through the rendered diagram.
 *
 * A target is only allowed when its scheme is http/https and every IP address the
 * host resolves to is a public, routable address. Because DNS may return several
 * records (and to defeat DNS-rebinding), the request is rejected if *any* resolved
 * address is non-public.
 */
object RemoteIncludeGuard {

    /** Returns true if [rawUrl] is safe to fetch server-side. */
    fun isAllowed(rawUrl: String): Boolean {
        val url = try {
            URL(rawUrl)
        } catch (e: Exception) {
            return false
        }

        val scheme = url.protocol?.lowercase()
        if (scheme != "http" && scheme != "https") {
            return false
        }

        val host = url.host
        if (host.isNullOrBlank()) {
            return false
        }

        val addresses = try {
            InetAddress.getAllByName(host)
        } catch (e: Exception) {
            return false
        }

        return addresses.isNotEmpty() && addresses.none { isBlocked(it) }
    }

    /** Returns true for loopback, link-local, private and other non-public addresses. */
    fun isBlocked(address: InetAddress): Boolean {
        if (address.isAnyLocalAddress ||    // 0.0.0.0, ::
            address.isLoopbackAddress ||    // 127.0.0.0/8, ::1
            address.isLinkLocalAddress ||   // 169.254.0.0/16 (incl. cloud metadata), fe80::/10
            address.isSiteLocalAddress ||   // 10/8, 172.16/12, 192.168/16
            address.isMulticastAddress      // 224.0.0.0/4, ff00::/8
        ) {
            return true
        }

        val bytes = address.address
        when (bytes.size) {
            4 -> {
                val b0 = bytes[0].toInt() and 0xff
                val b1 = bytes[1].toInt() and 0xff
                val b2 = bytes[2].toInt() and 0xff
                // 100.64.0.0/10 carrier-grade NAT
                if (b0 == 100 && b1 in 64..127) return true
                // 192.0.0.0/24 IETF protocol assignments
                if (b0 == 192 && b1 == 0 && b2 == 0) return true
                // 198.18.0.0/15 benchmarking
                if (b0 == 198 && (b1 == 18 || b1 == 19)) return true
            }
            16 -> {
                // Unique local addresses fc00::/7
                if ((bytes[0].toInt() and 0xfe) == 0xfc) return true
            }
        }
        return false
    }
}
