package com.github.michael72.pumlsrv

import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL

/**
 * Helper class to open a URL connection with a configured proxy setting.
 */
object ConnectionHelper {
    
    private fun getConnection(url: URL, useProxy: Boolean): HttpURLConnection {
        val httpProxy = System.getenv("HTTP_PROXY")
        var proxySetting = if (url.protocol.lowercase().contains("https")) {
            System.getenv("HTTPS_PROXY")
        } else {
            httpProxy
        }
        
        if (useProxy && proxySetting == null && httpProxy != null) {
            proxySetting = httpProxy // http is fallback for https
        }
        
        return if (useProxy && proxySetting != null && proxySetting.contains(":")) {
            val idx = proxySetting.lastIndexOf(':')
            val proxy = Proxy(
                Proxy.Type.HTTP, 
                InetSocketAddress(
                    // ip and port
                    proxySetting.substring(proxySetting.lastIndexOf('/', idx) + 1, idx),
                    proxySetting.substring(idx + 1).toInt()
                )
            )
            url.openConnection(proxy) as HttpURLConnection
        } else {
            url.openConnection() as HttpURLConnection
        }
    }
    
    fun getConnection(url: URL): HttpURLConnection = getConnection(url, true)
    
    fun getLocalConnection(url: URL): HttpURLConnection = getConnection(url, false)
    
    private fun getContent(httpUrl: String, useProxy: Boolean): InputStream? {
        return try {
            val url = URL(httpUrl)
            val con = getConnection(url, useProxy)
            con.content as? InputStream
        } catch (ioe: IOException) {
            null
        }
    }
    
    fun getLocalContent(httpUrl: String): InputStream? = getContent(httpUrl, false)
    
    fun getContent(httpUrl: String): InputStream? = getContent(httpUrl, true)
}