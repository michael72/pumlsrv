package com.github.michael72.pumlsrv

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.zip.DeflaterOutputStream
import java.util.zip.GZIPOutputStream

class AppPostTest {

    companion object {
        private const val TEST_PORT = 18765
        private lateinit var appInstance: App

        private val HELLO_BOB = """
            @startuml
            Bob -> Alice : hello
            @enduml
        """.trimIndent()

        @JvmStatic
        @BeforeAll
        fun setup() {
            val params = AppParams(portStart = TEST_PORT, noStore = true)
            appInstance = App(params)
            appInstance.listen(TEST_PORT)
        }

        @JvmStatic
        @AfterAll
        fun teardown() {
            // Javalin stop is handled via the app instance
        }
    }

    private val client = HttpClient.newHttpClient()
    private val baseUrl = "http://localhost:$TEST_PORT"

    @Test
    fun testPostSvgAtFormatPath() {
        val response = postDiagram("/svg", HELLO_BOB)
        assertEquals(200, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("image/svg+xml"))
        val body = String(response.body())
        assertTrue(body.contains("<svg"), "Response should contain SVG content")
    }

    @Test
    fun testPostSvgAtFormatPathWithTrailingSlash() {
        val response = postDiagram("/svg/", HELLO_BOB)
        assertEquals(200, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("image/svg+xml"))
    }

    @Test
    fun testPostPng() {
        val response = postDiagram("/png", HELLO_BOB)
        assertEquals(200, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("image/png"))
        // PNG starts with magic bytes
        val body = response.body()
        assertTrue(body.size > 8, "PNG response should have content")
        assertEquals(0x89.toByte(), body[0], "PNG should start with 0x89")
        assertEquals(0x50.toByte(), body[1], "PNG second byte should be 0x50 (P)")
    }

    @Test
    fun testPostTxt() {
        val response = postDiagram("/txt", HELLO_BOB)
        assertEquals(200, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("text/plain"))
        val body = String(response.body())
        assertTrue(body.isNotEmpty(), "TXT response should have content")
    }

    @Test
    fun testPostAtPlantumlPath() {
        val response = postDiagram("/plantuml/svg", HELLO_BOB)
        assertEquals(200, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("image/svg+xml"))
        val body = String(response.body())
        assertTrue(body.contains("<svg"), "Response should contain SVG content")
    }

    @Test
    fun testPostAtPlantumlPathWithTrailingSlash() {
        val response = postDiagram("/plantuml/svg/", HELLO_BOB)
        assertEquals(200, response.statusCode())
        assertTrue(response.headers().firstValue("Content-Type").orElse("").contains("image/svg+xml"))
    }

    @Test
    fun testPostGzipCompressed() {
        val compressed = gzipCompress(HELLO_BOB.toByteArray(Charsets.UTF_8))
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/svg"))
            .header("Content-Type", "text/plain")
            .header("Content-Encoding", "gzip")
            .POST(HttpRequest.BodyPublishers.ofByteArray(compressed))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
        assertEquals(200, response.statusCode())
        val body = String(response.body())
        assertTrue(body.contains("<svg"), "Gzipped POST should return SVG content")
    }

    @Test
    fun testPostDeflateCompressed() {
        val compressed = deflateCompress(HELLO_BOB.toByteArray(Charsets.UTF_8))
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/svg"))
            .header("Content-Type", "text/plain")
            .header("Content-Encoding", "deflate")
            .POST(HttpRequest.BodyPublishers.ofByteArray(compressed))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
        assertEquals(200, response.statusCode())
        val body = String(response.body())
        assertTrue(body.contains("<svg"), "Deflated POST should return SVG content")
    }

    @Test
    fun testPostEmptyBody() {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl/svg"))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofByteArray(ByteArray(0)))
            .build()
        val response = client.send(request, HttpResponse.BodyHandlers.ofByteArray())
        assertEquals(400, response.statusCode())
    }

    @Test
    fun testPostUnsupportedFormatViaPlantumlPath() {
        val response = postDiagram("/plantuml/pdf", HELLO_BOB)
        assertEquals(400, response.statusCode())
        val body = String(response.body())
        assertTrue(body.contains("Unsupported format"))
    }

    private fun postDiagram(path: String, source: String): HttpResponse<ByteArray> {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("$baseUrl$path"))
            .header("Content-Type", "text/plain")
            .POST(HttpRequest.BodyPublishers.ofString(source))
            .build()
        return client.send(request, HttpResponse.BodyHandlers.ofByteArray())
    }

    private fun gzipCompress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }

    private fun deflateCompress(data: ByteArray): ByteArray {
        val bos = ByteArrayOutputStream()
        DeflaterOutputStream(bos).use { it.write(data) }
        return bos.toByteArray()
    }
}
