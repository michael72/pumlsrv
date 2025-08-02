package com.github.michael72.pumlsrv

import java.io.IOException
import java.nio.charset.StandardCharsets
import java.nio.file.Files

/**
 * Converts UML requests to the plantuml server to images.
 */
object PumlApp {
    private const val STARTUML = "@startuml"

    private fun toUml(content: String): String {
        return UmlConverter.decode(content)
    }

    private fun checkStartUml(umlBuf: StringBuilder) {
        // Encapsulate the UML syntax if necessary
        if (umlBuf.isNotEmpty() && umlBuf[0] != '@') {
            umlBuf.insert(0, "@startuml\n")
            if (umlBuf.isNotEmpty() && umlBuf[umlBuf.length - 1] != '\n') {
                umlBuf.append("\n")
            }
            umlBuf.append("@enduml\n")
        }
    }

    private fun stripStartUml(uml: String): String {
        val idx = uml.indexOf(STARTUML)
        val idxEnd = uml.lastIndexOf("@enduml")
        
        return when {
            idx != -1 && idxEnd != -1 -> uml.substring(idx + STARTUML.length, idxEnd)
            idx != -1 -> uml.substring(idx + STARTUML.length)
            idxEnd != -1 -> uml.substring(0, idxEnd)
            else -> uml
        }
    }

    fun toImage(umlParts: String, idx: Int, params: AppParams, imageType: String): ConverterResult {
        val umlBuf = StringBuilder()
        val newpage = "\nnewpage\n"
        val parts = umlParts.split(newpage)
        
        parts.forEachIndexed { partIdx, uml ->
            when (params.outputMode) {
                AppParams.OutputMode.Dark -> umlBuf.append(Style.darkTheme())
                AppParams.OutputMode.Light -> umlBuf.append(Style.lightTheme())
                AppParams.OutputMode.Default -> { /* No additional styling */ }
            }
            
            params.includeFile?.let { file ->
                if (params.reload) {
                    try {
                        val inc = String(Files.readAllBytes(file.toPath()), StandardCharsets.US_ASCII)
                        umlBuf.append(stripStartUml(inc))
                    } catch (e: IOException) {
                        e.printStackTrace()
                    }
                } else {
                    umlBuf.append("!include ${file.absolutePath}")
                }
                umlBuf.append("\n")
            }
            
            if (params.isMonoChrome) {
                umlBuf.append("skinparam monochrome true\n")
            }
            
            if (umlBuf.isNotEmpty() || parts.isNotEmpty()) {
                val strippedUml = stripStartUml(uml)
                umlBuf.append(strippedUml)
            }
            
            if (parts.isNotEmpty() && partIdx < parts.size - 1) {
                umlBuf.append(newpage)
            }
        }
        
        checkStartUml(umlBuf)

        var result = UmlConverter.toImage(umlBuf.toString(), idx, imageType)
        if (result.isError) {
            umlBuf.setLength(0)
            umlBuf.append(umlParts)
            checkStartUml(umlBuf)
            result = UmlConverter.toErrorResult(umlBuf.toString(), idx)
        }
        return result
    }

    fun toImage(parseUrl: ParseUrl, params: AppParams): ConverterResult {
        return toImage(toUml(parseUrl.content), parseUrl.index, params, parseUrl.imageType)
    }
}