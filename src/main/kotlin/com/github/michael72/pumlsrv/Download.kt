package com.github.michael72.pumlsrv

import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.regex.Pattern

/**
 * Check for updates on plantuml jar and download newest version.
 */
object Download {
    private const val MAVEN_BASE = "https://repo1.maven.org/maven2/net/sourceforge/plantuml/plantuml"
    private const val BLOCK_SIZE = 4096

    fun getJar(saveTo: Path): String? {
        println("Checking for updates")
        
        // Get the HTML content from Maven repository
        val content = ConnectionHelper.getContent(MAVEN_BASE) ?: return null
        val htmlContent = getContent(content)
        
        // Parse HTML to find the newest version
        val newestVersion = findNewestVersion(htmlContent) ?: return null
        println("Found newest version: $newestVersion")
        
        val filename = "plantuml-$newestVersion.jar"
        val targetFile = Paths.get(saveTo.toString(), filename)
        
        if (Files.exists(targetFile)) {
            println("Already got newest plantuml file $filename")
            return filename
        }
        
        // Download the jar file
        val downloadUrl = "$MAVEN_BASE/$newestVersion/plantuml-$newestVersion.jar"
        println("Downloading $downloadUrl ...")
        
        return try {
            downloadFile(URL(downloadUrl), filename, saveTo.toString())
        } catch (t: Throwable) {
            System.err.println("Failed to download: ${t.message}")
            null
        }
    }

    private fun findNewestVersion(htmlContent: String): String? {
        // Pattern to match version directories with dates
        // Example: <a href="1.2025.4/" title="1.2025.4/">1.2025.4/</a>                                         2025-06-28 11:27         -
        val pattern = Pattern.compile("""<a href="([^"]+/)"\s+title="[^"]+">([^<]+)</a>\s+(\d{4}-\d{2}-\d{2}\s+\d{2}:\d{2})""")
        val matcher = pattern.matcher(htmlContent)
        
        var newestVersion: String? = null
        var newestDate: LocalDateTime? = null
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        
        while (matcher.find()) {
            val versionDir = matcher.group(1)
            val version = matcher.group(2)
            val dateStr = matcher.group(3)
            
            // Skip parent directory and non-version directories
            if (versionDir == "../" || !version.matches(Regex("""\d+\.\d{4}\.\d+/?"""))) {
                continue
            }
            
            try {
                val date = LocalDateTime.parse(dateStr, dateFormatter)
                if (newestDate == null || date.isAfter(newestDate)) {
                    newestDate = date
                    newestVersion = version.removeSuffix("/")
                }
            } catch (e: Exception) {
                // Skip entries with unparseable dates
                continue
            }
        }
        
        return newestVersion
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