package com.github.michael72.pumlsrv;

import java.io.File;
import java.util.prefs.Preferences;

public class AppParams {
  public enum OutputMode {
    Default, Dark, Light
  }

  private int port;
  int offset;
  File includeFile;
  final boolean reload;
  OutputMode outputMode;
  boolean isMonoChrome;
  final boolean showBrowser;
  final boolean noStore;
  boolean checkForUpdates;

  public AppParams(final int port, final int offset, final File includeFile, final boolean reload,
      final OutputMode outputMode, final boolean isMonoChrome, final boolean showBrowser, final boolean noStore,
      final boolean checkForUpdates) {
    this.port = port;
    this.offset = offset;
    this.includeFile = includeFile;
    this.reload = reload;
    this.outputMode = outputMode;
    this.isMonoChrome = isMonoChrome;
    this.showBrowser = showBrowser;
    this.noStore = noStore;
    this.checkForUpdates = checkForUpdates;
  }

  public AppParams next() {
    ++offset;
    return this;
  }

  public AppParams same() {
    --port;
    ++offset;
    return this;
  }

  public int port() {
    return this.port + this.offset;
  }

  public void setPort(int newPort) {
    this.port = newPort;
    this.offset = 0;
  }

  public void swapDarkMode() {
    this.outputMode = (this.outputMode != OutputMode.Dark) ? OutputMode.Dark : OutputMode.Default;
  }

  public void swapLightMode() {
    this.outputMode = (this.outputMode != OutputMode.Light) ? OutputMode.Light : OutputMode.Default;
  }

  public void setDefaultMode() {
    this.outputMode = OutputMode.Default;
  }

  public void store() {
    if (this.noStore) {
      return;
    }
    Preferences prefs = Preferences.userNodeForPackage(AppParams.class);
    prefs.putInt("port", port());
    prefs.put("include", includeFile != null ? includeFile.getAbsolutePath() : "");
    prefs.put("outputMode", outputMode.toString());
    prefs.putBoolean("isMonoChrome", isMonoChrome);
    prefs.putBoolean("checkForUpdates", checkForUpdates);
  }

  public void load() {
    if (this.noStore) {
      return;
    }
    Preferences prefs = Preferences.userNodeForPackage(AppParams.class);
    this.port = prefs.getInt("port", port());
    String inc = prefs.get("include", "");
    this.includeFile = (inc.length() > 0) ? new File(inc) : null;
    this.outputMode = OutputMode.valueOf(prefs.get("outputMode", outputMode.toString()));
    this.isMonoChrome = prefs.getBoolean("isMonoChrome", isMonoChrome);
    this.checkForUpdates = prefs.getBoolean("checkForUpdates", checkForUpdates);
  }

  public void loadPort() {
    Preferences prefs = Preferences.userNodeForPackage(AppParams.class);
    this.port = prefs.getInt("port", port());
  }
}
