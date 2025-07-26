package com.github.michael72.pumlsrv

import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

/**
 * Check for updates on plantuml jar and download newest version.
 */
object Download {
    private const val ROOT = "https://sourceforge.net/projects/plantuml"
    private const val RSS = "$ROOT/rss?path=/"
    private const val BLOCK_SIZE = 4096

    fun getJar(saveTo: Path): String? {
        println("Checking for updates")
        // Check on RSS feed for newest available version
        val content = ConnectionHelper.getContent(RSS) ?: return null
        
        val lines = getContent(content)
        val idxJarEnd = lines.indexOf(".jar]") + 4
        if (idxJarEnd == 3) return null
        
        val idxJar = lines.lastIndexOf("[", idxJarEnd) + 1
        if (idxJar == 0) return null
        
        val jar = lines.substring(idxJar, idxJarEnd)
        val filename = jar.substring(jar.lastIndexOf("/") + 1)
        
        if (Files.exists(Paths.get(saveTo.toString() + filename))) {
            println("Already got newest plantuml file $filename")
            return filename
        }
        
        // Download the jar file
        // sf sometimes is really slow - better download at mvnrepository
        val download = "https://repo1.maven.org/maven2/net/sourceforge/plantuml/plantuml" + 
                jar.replace("plantuml.", "plantuml-")
        println("Downloading $download ...")
        
        var result: String? = null
        try {
            result = downloadFile(URL(download), filename, saveTo.toString())
        } catch (t: Throwable) {
            // Ignore first failure
        }
        
        if (result == null) {
            val downloadSf = "$ROOT/files$jar/download"
            println("Retrying download at $downloadSf ...")
            result = downloadFile(URL(downloadSf), filename, saveTo.toString())
        }
        
        return result
    }

    private fun writeTo(out: OutputStream, input: InputStream, progress: ((Int) -> Unit)?) {
        val buf = ByteArray(BLOCK_SIZE)
        var readTotal = 0
        
        var bytesRead = input.read(buf)
        while (bytesRead != -1) {
            out.write(buf, 0, bytesRead)
            if (progress != null) {
                readTotal += bytesRead
                progress(readTotal)
            }
            bytesRead = input.read(buf)
        }
    }

    fun getContent(content: InputStream): String {
        val bos = ByteArrayOutputStream()
        writeTo(bos, content, null)
        return String(bos.toByteArray(), Charsets.UTF_8)
    }

    private fun downloadFile(url: URL, fileName: String, saveDir: String): String? {
        var result: String? = null
        
        val path = "${File(saveDir).absolutePath}${File.separator}$fileName"
        val tmp = File("$path~")
        if (tmp.exists()) {
            tmp.delete()
        }

        val con = ConnectionHelper.getConnection(url)
        
        if (con.responseCode == HttpURLConnection.HTTP_OK) {
            con.inputStream.use { inputStream ->
                FileOutputStream(tmp).use { out ->
                    // Download file and print progress
                    val tick = 5 // print every 2%
                    val part = con.contentLength / (tick * 10)
                    var next = 1
                    print("Progress %: 0")
                    
                    writeTo(out, inputStream) { progress ->
                        if (progress > next * part) {
                            print(if (next % tick == 0) "${next / tick * 10}" else ".")
                            next++
                        }
                    }
                    println()
                }
            }
            tmp.renameTo(File(path))
            result = fileName
        } else {
            System.err.println("No file downloaded. Server replied HTTP code: ${con.responseCode}")
        }
        
        con.disconnect()
        return result
    }
}