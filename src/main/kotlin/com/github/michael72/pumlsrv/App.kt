package com.github.michael72.pumlsrv

import io.javalin.Javalin
import io.javalin.http.Context
import io.javalin.http.HttpStatus
import java.io.ByteArrayInputStream
import java.io.IOException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

/**
 * Simple implementation of HTTP Server that handles PlantUML requests 
 * both as get and post using Javalin.
 * The PlantUML specific requests are forwarded to PumlApp.
 * This App can also handle configuration requests that are sent from the main 
 * configuration HTML page.
 */
class App(private val params: AppParams) {
    
    private val mediaTypes = mapOf(
        "svg" to "image/svg+xml",
        "png" to "image/png", 
        "eps" to "application/postscript",
        "epstext" to "text/plain",
        "txt" to "text/plain"
    )
    
    private var javalinApp: Javalin? = null
    private var oldJavalinApp: Javalin? = null

    fun listen(port: Int): Javalin {
        val app = Javalin.create { config ->
            config.http.defaultContentType = "text/html; charset=utf-8"
            config.showJavalinBanner = false
        }
        
        setupRoutes(app)
        
        javalinApp = app.start(port)
        return javalinApp!!
    }
    
    private fun setupRoutes(app: Javalin) {
        // PlantUML GET requests (encoded in URL)
        app.get("/plantuml/*") { ctx -> handlePlantumlRequest(ctx) }

        // PlantUML POST requests (raw source in body)
        // Support POST at /{format} and /{format}/ matching the official PlantUML server
        for (format in mediaTypes.keys) {
            app.post("/$format") { ctx -> handlePostRender(ctx, format) }
            app.post("/$format/") { ctx -> handlePostRender(ctx, format) }
        }
        app.post("/plantuml/*") { ctx -> handlePlantumlPostRequest(ctx) }

        // Configuration routes
        app.get("/exit") { ctx -> handleExit(ctx) }
        app.get("/mono") { ctx -> handleMonochrome(ctx) }
        app.get("/dark") { ctx -> handleDark(ctx) }
        app.get("/light") { ctx -> handleLight(ctx) }
        app.get("/default") { ctx -> handleDefault(ctx) }
        app.get("/move_to") { ctx -> handleMovePort(ctx) }
        app.get("/check_updates") { ctx -> handleCheckUpdates(ctx) }
        app.get("/show_browser") { ctx -> handleShowBrowser(ctx) }
        app.get("/favicon.ico") { ctx -> handleFavicon(ctx) }
        app.get("/") { ctx -> handleRoot(ctx) }
        
        // 404 handler
        app.error(404) { ctx ->
            println("URL not found: ${ctx.path()}")
            ctx.status(HttpStatus.NOT_FOUND).result("Page not found")
        }
    }
    
    private fun handlePlantumlRequest(ctx: Context) {
        try {
            val path = ctx.path().removePrefix("/plantuml/")
            val parseUrl = ParseUrl(path)
            val convResult = PumlApp.toImage(parseUrl, params)
            
            ctx.contentType(mediaTypes[convResult.imageType] ?: "text/plain")
            ctx.result(convResult.bytes)
        } catch (ex: IOException) {
            ex.printStackTrace()
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType("text/plain")
                .result("Error: could not parse UML code")
        }
    }
    
    private fun handlePlantumlPostRequest(ctx: Context) {
        val path = ctx.path().removePrefix("/plantuml/").trimEnd('/')
        val parts = path.split("/")
        val format = parts[0]
        if (format !in mediaTypes) {
            ctx.status(HttpStatus.BAD_REQUEST)
                .contentType("text/plain")
                .result("Unsupported format: $format. Supported: ${mediaTypes.keys.joinToString()}")
            return
        }
        handlePostRender(ctx, format)
    }

    private fun handlePostRender(ctx: Context, imageType: String) {
        try {
            val bodyBytes = ctx.bodyAsBytes()
            if (bodyBytes.isEmpty()) {
                ctx.status(HttpStatus.BAD_REQUEST)
                    .contentType("text/plain")
                    .result("Error: empty request body")
                return
            }
            val umlSource = decodePostBody(bodyBytes, ctx)
            val convResult = PumlApp.toImage(umlSource, 0, params, imageType)
            ctx.contentType(mediaTypes[imageType] ?: "text/plain")
            ctx.result(convResult.bytes)
        } catch (ex: Exception) {
            ex.printStackTrace()
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType("text/plain")
                .result("Error: could not process UML diagram")
        }
    }

    private fun decodePostBody(bodyBytes: ByteArray, ctx: Context): String {
        val encoding = ctx.header("Content-Encoding")?.lowercase()
        val decompressedBytes = when (encoding) {
            "gzip" -> GZIPInputStream(ByteArrayInputStream(bodyBytes)).readBytes()
            "deflate" -> InflaterInputStream(ByteArrayInputStream(bodyBytes)).readBytes()
            else -> bodyBytes
        }
        return String(decompressedBytes, Charsets.UTF_8)
    }
    
    private fun handleExit(ctx: Context) {
        exitLater(50)
        ctx.contentType("text/plain").result("pumlsrv exiting...\nBYE!")
    }
    
    private fun handleMovePort(ctx: Context) {
        checkOldServer(true)
        val portStr = ctx.queryParam("port")
        val movePort = portStr?.toIntOrNull() ?: return ctx.redirect("/")
        
        if (movePort != params.port()) {
            params.setPort(movePort)
            listen(movePort)
            
            val userAgent = ctx.header("User-Agent") ?: ""
            val isIE = userAgent.contains("Trident")
            val url = "http://localhost:${params.port()}"
            
            val html = if (isIE) {
                """<html><head><script>window.location="$url";</script></head></html>"""
            } else {
                """<html><head><meta http-equiv="refresh" content="0; url=$url"/></head></html>"""
            }
            
            ctx.status(if (isIE) HttpStatus.OK else HttpStatus.SEE_OTHER)
                .contentType("text/html")
                .result(html)
        } else {
            redirectToRoot(ctx)
        }
    }
    
    private fun handleMonochrome(ctx: Context) {
        params.isMonoChrome = !params.isMonoChrome
        redirectToRoot(ctx)
    }
    
    private fun handleDark(ctx: Context) {
        params.swapDarkMode()
        redirectToRoot(ctx)
    }
    
    private fun handleLight(ctx: Context) {
        params.swapLightMode()
        redirectToRoot(ctx)
    }
    
    private fun handleDefault(ctx: Context) {
        params.setDefaultMode()
        redirectToRoot(ctx)
    }
    
    private fun handleCheckUpdates(ctx: Context) {
        params.checkForUpdates = !params.checkForUpdates
        redirectToRoot(ctx)
    }
    
    private fun handleShowBrowser(ctx: Context) {
        params.showBrowser = !params.showBrowser
        redirectToRoot(ctx)
    }
    
    private fun handleFavicon(ctx: Context) {
        ctx.contentType("image/x-icon").result(Resources.favicon)
    }
    
    private fun handleRoot(ctx: Context) {
        checkOldServer(false)
        params.store()
        val html = MainHtml(params).html()
        ctx.contentType("text/html; charset=utf-8").result(html)
    }
    
    private fun redirectToRoot(ctx: Context) {
        val html = """
            <html>
                <head>
                    <script>window.location="/"</script>
                </head>
                <body/>
            </html>
        """.trimIndent()
        ctx.contentType("text/html").result(html)
    }
    
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") 
    private fun checkOldServer(shutdown: Boolean) {
        oldJavalinApp?.let { oldApp ->
            synchronized(oldApp) {
                try {
                    if (shutdown) {
                        // Back and forth and old server still running? wait a little...
                        Thread.sleep(1000)
                    } else {
                        // Notify shutdown thread that new server port is up
                        (oldApp as java.lang.Object).notify()
                    }
                } catch (t: Throwable) {
                    // Ignore
                }
            }
        }
        
        if (shutdown) {
            shutdownServer()
        }
    }
    
    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN") 
    private fun shutdownServer() {
        oldJavalinApp = javalinApp
        
        Thread({
            synchronized(oldJavalinApp!!) {
                try {
                    (oldJavalinApp!! as java.lang.Object).wait(1000)
                    oldJavalinApp!!.stop()
                    oldJavalinApp = null
                } catch (e: InterruptedException) {
                    // Ignore
                }
            }
        }, "ShutdownOldPort").start()
    }
    
    companion object {
        fun exitLater(millis: Long) {
            val executor = Executors.newSingleThreadScheduledExecutor()
            executor.schedule({
                synchronized(App::class.java) {
                    try {
                        Thread.sleep(millis)
                    } catch (e: InterruptedException) {
                        // Ignore
                    }
                    System.exit(0)
                }
            }, millis, TimeUnit.MILLISECONDS)
        }
    }
}