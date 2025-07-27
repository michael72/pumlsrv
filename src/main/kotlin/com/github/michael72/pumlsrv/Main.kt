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

    override fun call(): Int {
        if (darkMode && lightMode) {
            System.err.println("Cannot use dark and light both together - using dark mode.")
        }
        
        var outputMode = when {
            darkMode -> AppParams.OutputMode.Dark
            lightMode -> AppParams.OutputMode.Light
            else -> AppParams.OutputMode.Default
        }
        
        val modeEnv = System.getenv("PUMLSRV_MODE")
        if (modeEnv != null) {
            try {
                val normalizedMode = modeEnv.trim().lowercase().replaceFirstChar { it.uppercase() }
                outputMode = AppParams.OutputMode.valueOf(normalizedMode)
            } catch (ex: IllegalArgumentException) {
                System.err.println("Unsupported mode in env PUMLSRV_MODE: $modeEnv, using default mode.")
            }
        }

        includeFile?.let {
            println("Using include file $it")
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
            loadDynamicJar = !noDynamicJar
        )
        
        if (clear) {
            params.loadPort()
        } else {
            params.load()
        }
        
        return AppStarter.startOnPort(params)
    }
}