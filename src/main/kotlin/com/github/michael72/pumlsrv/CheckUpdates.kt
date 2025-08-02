package com.github.michael72.pumlsrv

/**
 * Check for updates of pumlsrv on github.
 */
object CheckUpdates {
    private var result: String? = null

    fun checkUpdates(): String {
        if (result != null) {
            return result!!
        }

        try {
            val latest = "https://github.com/michael72/pumlsrv/releases/latest"
            val content = ConnectionHelper.getContent(latest) ?: return ""
            
            val lines = Download.getContent(content)
            var idx = lines.indexOf("Latest release")
            if (idx != -1) {
                idx = lines.indexOf("title=", idx)
                if (idx != -1) {
                    idx = lines.indexOf('"', idx) + 1
                }
            }
            
            if (idx > 0) {
                val tagVersion = lines.substring(idx, lines.indexOf('"', idx))
                if (versionIsNewer(tagVersion)) {
                    val idxDetails = lines.indexOf("<div class=\"markdown-body\">")
                    val details = lines.substring(idxDetails, lines.indexOf("<details", idxDetails))
                    var idxTitle = lines.indexOf("class=\"release-header\"")
                    idxTitle = lines.indexOf(tagVersion, idxTitle)
                    idxTitle = lines.indexOf(">", idxTitle) + 1
                    val title = lines.substring(idxTitle, lines.indexOf('<', idxTitle))
                    
                    result = "<h2>Release $tagVersion available</h2>" +
                            "<h3>$title</h3>" +
                            details
                    return result!!
                }
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }

        result = ""
        return result!!
    }

    private fun versionNum(version: String): Int {
        var v = version.trim()
        if (v.startsWith('v')) {
            v = v.substring(1)
        }
        val parts = v.split(".")
        var ret = 0
        for (i in 0..2) {
            ret = 10 * ret + (if (i < parts.size) parts[i].toInt() else 0)
        }
        return ret
    }

    private fun versionIsNewer(checkVersion: String): Boolean {
        return versionNum(checkVersion) > versionNum(Resources.version)
    }
}