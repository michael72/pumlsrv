package com.github.michael72.pumlsrv;

import java.io.File;
import java.util.concurrent.Callable;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import com.github.michael72.pumlsrv.AppParams.OutputMode;

@Command(description = "An efficient and small implementation of a PlantUML server.", name = "pumlsrv", mixinStandardHelpOptions = true, version = Resources.version)
public class Main implements Callable<Integer> {
  private static final int DEFAULT_PORT = 8080;

  private static int defaultPort() {
    final String portEnv = System.getenv("PUMLSRV_PORT");
    if (portEnv != null) {
      return Integer.parseInt(portEnv);
    }
    return DEFAULT_PORT;
  }

  @Parameters(index = "0", paramLabel = "PORT", defaultValue = ""
      + DEFAULT_PORT, description = "Port of the http server to connect to", arity = "0..1")
  private Integer used_port = defaultPort();
  @Option(names = { "-D", "--dark" }, description = "Switch to dark mode")
  private boolean dark_mode;
  @Option(names = { "-L", "--light" }, description = "Switch to light mode")
  private boolean light_mode;
  @Option(names = { "-M", "--monochrome" }, description = "Switch to monochrome mode")
  private boolean monochrome_mode;
  @Option(names = { "-i", "--include" }, description = "Additional style to include for each UML")
  private File include_file;
  @Option(names = { "-r", "--reload" }, description = "Reload the include file on every access")
  private boolean reload;
  @Option(names = { "-n",
      "--nosettings" }, description = "Do not use and store current settings. By default the last settings are saved and used on next startup (without parameters).")
  private boolean noSettings;
  @Option(names = { "-c", "--clear" }, description = "Clear default settings (except used port)")
  private boolean clear;
  @Option(names = { "-N",
      "--nobrowser" }, description = "Do not show browser on startup. By default the browser is opened on the current root page.")
  private boolean noBrowser;
  @Option(names = { "-u", "--noupdates" }, description = "Do not check for updates of plantuml.jar and pumlsrv.")
  private boolean noUpdates;
  @Option(names = { "-j", "--nodynamicjar" }, description = "Do not try to load the plantuml.jar dynamically.")
  private boolean noDynamicJar;

  @Override
  public Integer call() throws Exception {
    if (this.dark_mode && this.light_mode) {
      System.err.println("Cannot use dark and light both together - using dark mode.");
    }
    OutputMode outputMode = this.dark_mode ? OutputMode.Dark
        : (this.light_mode ? OutputMode.Light : OutputMode.Default);
    String modeEnv = System.getenv("PUMLSRV_MODE");
    if (modeEnv != null) {
      modeEnv = modeEnv.trim().toLowerCase();
      modeEnv = modeEnv.substring(0, 1).toUpperCase() + modeEnv.substring(1);
      try {
        outputMode = OutputMode.valueOf(modeEnv);
      } catch (IllegalArgumentException ex) {
        System.err.println("Unsupported mode in env PUMLSRV_MODE: " + modeEnv + ", using default mode.");
      }
    }

    if (this.include_file != null) {
      System.out.println("Using include file " + this.include_file);
    }

    final AppParams params = new AppParams(used_port, 0, this.include_file, this.reload, outputMode,
        this.monochrome_mode, !this.noBrowser, this.noSettings, !this.noUpdates, !this.noDynamicJar);
    if (this.clear) {
      params.loadPort();
    } else {
      params.load();
    }
    return AppStarter.startOnPort(params);
  }

  static String[] theArgs;
  public static void main(String[] args) throws Exception {
    theArgs = args;
    int exitCode = new CommandLine(new Main()).execute(args);
    if (exitCode != 0) {
      System.exit(exitCode);
    }
  }
}