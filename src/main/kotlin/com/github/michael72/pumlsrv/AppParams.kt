package com.github.michael72.pumlsrv

import java.io.File
import java.util.prefs.Preferences

data class AppParams(
    var port: Int,
    var offset: Int = 0,
    val includeFile: File? = null,
    val reload: Boolean = false,
    var outputMode: OutputMode = OutputMode.Default,
    var isMonoChrome: Boolean = false,
    var showBrowser: Boolean = true,
    val noStore: Boolean = false,
    var checkForUpdates: Boolean = true,
    val loadDynamicJar: Boolean = true
) {
    enum class OutputMode {
        Default, Dark, Light
    }

    fun next(): AppParams {
        offset++
        return this
    }

    fun same(): AppParams {
        port--
        offset++
        return this
    }

    fun port(): Int = port + offset

    fun setPort(newPort: Int) {
        port = newPort
        offset = 0
    }

    fun swapDarkMode() {
        outputMode = if (outputMode != OutputMode.Dark) OutputMode.Dark else OutputMode.Default
    }

    fun swapLightMode() {
        outputMode = if (outputMode != OutputMode.Light) OutputMode.Light else OutputMode.Default
    }

    fun setDefaultMode() {
        outputMode = OutputMode.Default
    }

    fun store() {
        if (noStore) return
        
        val prefs = Preferences.userNodeForPackage(AppParams::class.java)
        prefs.putInt("port", port())
        prefs.put("include", includeFile?.absolutePath ?: "")
        prefs.put("outputMode", outputMode.toString())
        prefs.putBoolean("isMonoChrome", isMonoChrome)
        prefs.putBoolean("checkForUpdates", checkForUpdates)
        prefs.putBoolean("showBrowser", showBrowser)
    }

    fun load() {
        if (noStore) return
        
        val prefs = Preferences.userNodeForPackage(AppParams::class.java)
        port = prefs.getInt("port", port())
        val inc = prefs.get("include", "")
        val includeFileFromPrefs = if (inc.isNotEmpty()) File(inc) else null
        outputMode = OutputMode.valueOf(prefs.get("outputMode", outputMode.toString()))
        isMonoChrome = prefs.getBoolean("isMonoChrome", isMonoChrome)
        checkForUpdates = prefs.getBoolean("checkForUpdates", checkForUpdates)
        showBrowser = prefs.getBoolean("showBrowser", showBrowser)
    }

    fun loadPort() {
        val prefs = Preferences.userNodeForPackage(AppParams::class.java)
        port = prefs.getInt("port", port())
    }
}