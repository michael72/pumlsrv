package com.github.michael72.pumlsrv

import java.awt.Desktop
import java.io.File
import java.io.FilenameFilter
import java.lang.reflect.Method
import java.net.URI
import java.net.URL
import java.nio.file.Paths
import java.util.*
import kotlin.system.exitProcess

object AppStarter {
    private const val RETRIES = 10

    fun startOnPort(sp: AppParams): Int {
        var thread: Thread? = null
        
        if (sp.loadDynamicJar) {
            addPlantUmlJar(sp)
        }
        
        try {
            if (sp.showBrowser && Desktop.isDesktopSupported()) {
                thread = Thread({
                    try {
                        synchronized(this) {
                            await(3000)
                            if (sp.offset == 0) {
                                Desktop.getDesktop().browse(URI("http://localhost:${sp.port()}"))
                            }
                        }
                    } catch (e: InterruptedException) {
                        // Ignore
                    } catch (t: Throwable) {
                        t.printStackTrace()
                    }
                }, "BrowserOpener")
                thread.start()
            }
            
            if (sp.offset < RETRIES) {
                println("pumlserver: listening on http://localhost:${sp.port()}/plantuml")
                App(sp).listen(sp.port())
                return 0
            }
            
            println("no port found after $RETRIES tries")
            return -1
            
        } catch (ex: RuntimeException) {
            try {
                thread?.interrupt()
            } catch (t: Throwable) {
                // Ignore
            }
            
            if ("Server start-up failed!" == ex.message) {
                val urlPre = "http://localhost:${sp.port()}"
                try {
                    if (ConnectionHelper.getLocalContent("$urlPre/plantuml/txt/SoWkIImgAStDuN9KqBLJSE9oICrB0N81") != null) {
                        println("Another PlantUML server is running on port ${sp.port()} - stopping it!")
                        // Try to kill the other server
                        if (ConnectionHelper.getLocalContent("$urlPre/exit") != null) {
                            // Other server exited
                            synchronized(AppStarter::class.java) {
                                AppStarter::class.java.await(500)
                            }
                            return startOnPort(sp.same())
                        }
                    }
                    // Continue with next port
                    return startOnPort(sp.next())
                } catch (t: Throwable) {
                    t.printStackTrace()
                }
            }
            return -1
        }
    }

    @Synchronized
    private fun loadLibrary(jar: File, sp: AppParams) {
        val classLoader = ClassLoader.getSystemClassLoader()
        try {
            val method = classLoader.javaClass.getDeclaredMethod("addURL", URL::class.java)
            method.isAccessible = true
            method.invoke(classLoader, jar.toURI().toURL())
        } catch (t: Throwable) {
            try {
                val method = classLoader.javaClass.getDeclaredMethod("appendToClassPathForInstrumentation", String::class.java)
                method.isAccessible = true
                method.invoke(classLoader, jar.absolutePath)
            } catch (t2: Throwable) {
                // Java 14+ workaround - restart with classpath
                val args = mutableListOf<String>()
                val thisJar = File(
                    AppStarter::class.java.protectionDomain.codeSource.location.toURI().path
                ).absolutePath
                
                args.add("java")
                args.add("-cp")
                args.add("$thisJar${File.pathSeparator}${jar.name}")
                args.add(Main::class.java.name)
                args.addAll(Main.theArgs)
                args.add("-j")
                
                println("java14 hack - restarting with: ")
                args.forEach { print("$it ") }
                println()
                
                Runtime.getRuntime().exec(args.toTypedArray(), null, File(".").absoluteFile)
                println("Other job is running in background now - exiting.")
                println("pumlserver: listening on http://localhost:${sp.port()}/plantuml")
                exitProcess(0)
            }
        }
    }

    private var added = false

    fun addPlantUmlJar(sp: AppParams) {
        if (added) return
        
        var currentFile: String? = null
        val files = File(".").listFiles(FilenameFilter { _, name ->
            name.endsWith(".jar") && name.startsWith("plantuml.")
        })
        val filesEmpty = files == null || files.isEmpty()

        if (filesEmpty || sp.checkForUpdates) {
            try {
                currentFile = Download.getJar(Paths.get(""))
            } catch (t: Throwable) {
                t.printStackTrace()
                System.err.println("No update done - no internet connection. Exiting...")
            }
        }

        if (sp.checkForUpdates) {
            CheckUpdates.checkUpdates()
        }

        if (currentFile != null) {
            if (files != null && files.isNotEmpty()) {
                // Remove old files
                files.forEach { file ->
                    if (file.name != currentFile) {
                        file.delete()
                    }
                }
            }
        } else if (!filesEmpty) {
            // Use the newest of the files
            files!!.sort()
            currentFile = files.last().name
        }
        
        currentFile?.let { fileName ->
            try {
                loadLibrary(File(fileName), sp)
                added = true
            } catch (t: Throwable) {
                t.printStackTrace()
            }
        }
    }
}