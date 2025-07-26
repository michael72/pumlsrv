package com.github.michael72.pumlsrv

import java.io.File
import java.io.IOException

class MainHtml(private val params: AppParams) {
    
    fun localhost(): String = "http://localhost:${params.port()}"
    
    fun hello(): String = buildString {
        append("@startuml\ntitle:")
        append("pumlsrv ${Resources.version} running on port ${params.port()}")
        append("\nAlice -> Bob: Hello pumlsrv!\n@enduml\n")
    }
    
    private fun addLink(
        buf: StringBuilder,
        name: String,
        href: String,
        bg: String,
        bc: String,
        fg: String
    ) {
        val style = buildString {
            append("background-color: $bg;color: $fg;")
            append("margin-left: 6px; margin-right: 30px; margin-top: -6px; ")
            append("border-radius: 6px; border:2px solid $bc;")
            append("padding: 10px 10px;text-align: center;text-decoration: none;display: inline-block;")
        }
        buf.append("""<a style="$style" href="/$href">$name</a>""")
    }
    
    private fun addLink(buf: StringBuilder, mode: AppParams.OutputMode, bg: String, bc: String, fg: String) {
        val name = mode.toString().lowercase()
        addLink(buf, name, name, bg, bc, fg)
    }
    
    private fun addToggle(buf: StringBuilder, label: String, checked: Boolean, name: String) {
        val href = "/$name"
        buf.append("<td><label for=\"switch$name\">$label</label></td>")
        
        buf.append("""
            <td><label class="switch" id="switch$name">
            <input type="checkbox" ${if (checked) "checked" else ""}>
            <span class="slider round" id="updateSwitch$name"></span>
            </label></td>
        """.trimIndent())
        
        buf.append("""
            <script>
            function onUpdate$name() {
                window.location.href='$href';
            }
            function onUpdate2$name() {
                setTimeout(onUpdate$name,400);
            }
            document.getElementById('updateSwitch$name').onclick=onUpdate2$name;
            </script>
        """.trimIndent())
    }
    
    fun html(): ByteArray {
        return try {
            generateHtml().toByteArray()
        } catch (err: NoClassDefFoundError) {
            generateErrorHtml().toByteArray()
        }
    }
    
    private fun generateHtml(): String = buildString {
        // Create title
        append("<html><head>")
        append("<style>")
        append(Resources.switchCss)
        append("html *\n{\n   font-family: Arial;\n}\n")
        append("</style>")
        append("<title>pumlsrv ${Resources.version} configuration</title>")
        append("""<link rel="shortcut icon" href="${localhost()}/favicon.ico">""")
        append("</head>")
        append("<body>")
        append(Resources.pumlsrvSvg)
        
        try {
            // Show hello UML diagram in current style setting
            append("""<p style="margin-top: -3px;">""")
            val helloResult = PumlApp.toImage(hello(), 0, params, "svg")
            append(String(helloResult.bytes))
            append("</p>")
            
            // Mode selection
            append("<p>")
            AppParams.OutputMode.values().forEach { mode ->
                if (params.outputMode != mode) {
                    when (mode) {
                        AppParams.OutputMode.Dark -> 
                            addLink(this, mode, "#203562", "#81D4FA", "white")
                        AppParams.OutputMode.Light -> 
                            addLink(this, mode, "white", "#81D4FA", "black")
                        AppParams.OutputMode.Default -> 
                            addLink(this, mode, "fffdcf", "#a00000", "black")
                    }
                }
            }
            
            if (params.isMonoChrome) {
                addLink(this, "color", "mono", "blue", "red", "yellow")
            } else {
                addLink(this, "mono", "mono", "black", "gray", "white")
            }
            
            // Format items in table
            append("</p><table><tr>")
            
            // Show port entry field
            append("""
                <form class="form-inline" action="/move_to">
                <div class="form-group"><td>
                <label for="port">Port:</label>
                <input type="number" id="port" name="port" value="${params.port()}">
                </td></div><td>
                <input type="submit" value="Change">
                </td>
                </form>
            """.trimIndent())
            
            append("</tr><tr>")
            addToggle(this, "Check for updates on start", params.checkForUpdates, "check_updates")
            append("</tr><tr>")
            addToggle(this, "Open in browser on start", params.showBrowser, "show_browser")
            append("</tr></table></p>")
            
            if (params.checkForUpdates) {
                val updates = CheckUpdates.checkUpdates()
                if (updates.isNotEmpty()) {
                    append("</p><p></hr>")
                    append(updates)
                    append("""</p><p><a href="https://github.com/michael72/pumlsrv/releases/latest">Download latest release</a>""")
                }
            }
            
            append("</p></body></html>")
            
        } catch (e: IOException) {
            e.printStackTrace()
            append("<p><h2>Error generating PlantUML diagram</h2></p>")
            append("</body></html>")
        }
    }
    
    private fun generateErrorHtml(): String = buildString {
        val s = generateHtml().replace("#006680", "#802020")
        append(s)
        append("<p><h2>Error: Unable to start pumlsrv!</h2><p>")
        append("<p><b>No plantuml*.jar found in current directory and could not be downloaded from server</b></p>")
        append("<p>Current directory is ${File(".").absolutePath}</p>")
        append("""<p>Please check your internet connection and try again or download <a href="https://sourceforge.net/projects/plantuml/files">here</a></p>""")
        App.exitLater(200)
    }
}