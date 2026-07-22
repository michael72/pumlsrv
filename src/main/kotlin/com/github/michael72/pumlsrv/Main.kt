package com.github.michael72.pumlsrv

import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.File
import java.util.concurrent.Callable
import kotlin.system.exitProcess

@Command(
    description = ["An efficient and small implementation of a PlantUML server."],
    name = "pumlsrv",
    mixinStandardHelpOptions = true,
    version = [Resources.version]
)
class Main : Callable<Int> {
    
    companion object {
        private const val DEFAULT_PORT = 8080
        
        private fun defaultPort(): Int {
            val portEnv = System.getenv("PUMLSRV_PORT")
            return portEnv?.toIntOrNull() ?: DEFAULT_PORT
        }

        /**
         * Base color mode taken from the PUMLSRV_COLOR_MODE environment variable.
         * `light` is equivalent to `-L`, `dark` is equivalent to `-D`.
         * Explicit `-L` / `-D` parameters take precedence over this value.
         */
        private fun colorModeFromEnv(): AppParams.OutputMode {
            val colorMode = System.getenv("PUMLSRV_COLOR_MODE")?.trim() ?: return AppParams.OutputMode.Default
            return when (colorMode.lowercase()) {
                "" -> AppParams.OutputMode.Default
                "light" -> AppParams.OutputMode.Light
                "dark" -> AppParams.OutputMode.Dark
                else -> {
                    System.err.println(
                        "Unsupported mode in env PUMLSRV_COLOR_MODE: $colorMode - expected 'light' or 'dark', using default mode."
                    )
                    AppParams.OutputMode.Default
                }
            }
        }
        
        lateinit var theArgs: Array<String>
        
        @JvmStatic
        fun main(args: Array<String>) {
            theArgs = args
            val exitCode = CommandLine(Main()).execute(*args)
            if (exitCode != 0) {
                exitProcess(exitCode)
            }
        }
    }

    @Parameters(
        index = "0",
        paramLabel = "PORT",
        defaultValue = DEFAULT_PORT.toString(),
        description = ["Port of the http server to connect to"],
        arity = "0..1"
    )
    private var usedPort: Int = defaultPort()

    @Option(names = ["-D", "--dark"], description = ["Switch to dark mode"])
    private var darkMode: Boolean = false

    @Option(names = ["-L", "--light"], description = ["Switch to light mode"])
    private var lightMode: Boolean = false

    @Option(names = ["-M", "--monochrome"], description = ["Switch to monochrome mode"])
    private var monochromeMode: Boolean = false

    @Option(names = ["-i", "--include"], description = ["Additional style to include for each UML"])
    private var includeFile: File? = null

    @Option(names = ["-r", "--reload"], description = ["Reload the include file on every access"])
    private var reload: Boolean = false

    @Option(
        names = ["-n", "--nosettings"], 
        description = ["Do not use and store current settings. By default the last settings are saved and used on next startup (without parameters)."]
    )
    private var noSettings: Boolean = false

    @Option(names = ["-c", "--clear"], description = ["Clear default settings (except used port)"])
    private var clear: Boolean = false

    @Option(
        names = ["-N", "--nobrowser"], 
        description = ["Do not show browser on startup. By default the browser is opened on the current root page."]
    )
    private var noBrowser: Boolean = false

    @Option(names = ["-u", "--noupdates"], description = ["Do not check for updates of plantuml.jar and pumlsrv."])
    private var noUpdates: Boolean = false

    @Option(names = ["-j", "--nodynamicjar"], description = ["Do not try to load the plantuml.jar dynamically."])
    private var noDynamicJar: Boolean = false

    @Option(names = ["--debug"], description = ["Directory to write debug files (incoming .puml and rendered output) for each /plantuml/... request"])
    private var debugDir: File? = null

    override fun call(): Int {
        if (darkMode && lightMode) {
            System.err.println("Cannot use dark and light both together - using dark mode.")
        }
        
        // Base color mode from the PUMLSRV_COLOR_MODE environment variable.
        // Explicit -D / -L parameters override the environment variable.
        var outputMode = colorModeFromEnv()
        when {
            darkMode -> outputMode = AppParams.OutputMode.Dark
            lightMode -> outputMode = AppParams.OutputMode.Light
        }

        includeFile?.let {
            println("Using include file $it")
        }

        debugDir?.let {
            println("Debug mode: writing request/response files to $it")
        }

        val params = AppParams(
            portStart = usedPort,
            offset = 0,
            includeFile = includeFile,
            reload = reload,
            outputMode = outputMode,
            isMonoChrome = monochromeMode,
            showBrowser = !noBrowser,
            noStore = noSettings,
            checkForUpdates = !noUpdates,
            loadDynamicJar = !noDynamicJar,
            debugDir = debugDir
        )
        
        if (clear) {
            params.loadPort()
        } else {
            params.load()
        }
        
        return AppStarter.startOnPort(params)
    }
}