package com.github.michael72.pumlsrv

import net.sourceforge.plantuml.FileFormat
import net.sourceforge.plantuml.FileFormatOption
import net.sourceforge.plantuml.SourceStringReader
import net.sourceforge.plantuml.code.TranscoderUtil
import org.apache.commons.text.StringEscapeUtils
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

object UmlConverter {
    // Supported formats are as in plantuml server: png, svg, eps, epstext and txt.
    private val fileFormats = mapOf(
        "svg" to FileFormat.SVG,
        "png" to FileFormat.PNG,
        "eps" to FileFormat.EPS,
        "epstext" to FileFormat.EPS_TEXT,
        "txt" to FileFormat.UTXT
    )

    /**
     * Compress Plant-UML string with zlib.
     */
    fun encode(uml: String): String {
        return TranscoderUtil.getDefaultTranscoder().encode(uml.replace("\r\n", "\n"))
    }

    /**
     * Uncompress compressed string to the original Plant-UML string.
     */
    fun decode(source: String): String {
        // Build the UML source from the compressed part of the URL
        return TranscoderUtil.getDefaultTranscoder().decode(source)
    }

    /**
     * Convert Plant-UML string to an image.
     */
    fun toImage(uml: String, idx: Int, imageType: String): ConverterResult {
        val os = ByteArrayOutputStream()

        // Write the first image to "os"
        val desc = SourceStringReader(uml).outputImage(
            os, 
            idx, 
            FileFormatOption(fileFormats[imageType])
        )
        val isError = desc.description == "(Error)" && imageType == "svg"

        return ConverterResult(os.toByteArray(), desc.description, imageType, isError)
    }

    fun toErrorResult(uml: String, idx: Int): ConverterResult {
        // Re-write the SVG output with the actual text content
        // otherwise there is too much noise in the created image
        val result = toImage(uml, idx, "txt")
        var img = String(result.bytes, Charset.forName("UTF-8"))

        var maxLength = 0
        val arr = img.split("\n")
        val lines = mutableListOf<String>()
        
        for (line in arr) {
            val trimmedLine = line.trim()
            if (trimmedLine.isNotEmpty() && trimmedLine[0] != '@') {
                // Trim right and escape to HTML
                val escapedLine = StringEscapeUtils.escapeHtml4(trimmedLine.replace("\\s+$".toRegex(), ""))
                maxLength = maxOf(maxLength, escapedLine.length)
                lines.add(escapedLine)
            }
        }
        
        if (lines.size > 8) {
            val croppedList = lines.take(4).toMutableList()
            croppedList.addAll(lines.takeLast(4))
            lines.clear()
            lines.addAll(croppedList)
        }

        val yOffset = 20
        var y = yOffset
        val lineHeight = 25
        val charWidth = 8
        val height = y + lines.size * lineHeight
        val width = maxLength * charWidth

        img = buildString {
            append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>")
            append("<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" ")
            append("contentScriptType=\"application/ecmascript\" contentStyleType=\"text/css\" ")
            append("version=\"1.1\" zoomAndPan=\"magnify\" preserveAspectRatio=\"none\" ")
            append("height=\"$height\" width=\"$width\">\n")
            append("<text x=\"0\" y=\"$yOffset\" fill=\"red\">${lines[0]}\n")

            for (i in 1 until lines.size) {
                y += lineHeight
                append("<tspan x=\"10\" y=\"$y\">${lines[i]}</tspan>\n")
            }
            append("</text>\n</svg>")
        }

        return ConverterResult(img.toByteArray(), result.description, "svg", false)
    }
}