package com.github.michael72.pumlsrv;

import java.awt.Desktop;
import java.io.File;
import java.io.FilenameFilter;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;

public class AppStarter {
  private static final int RETRIES = 10;

  static int startOnPort(final AppParams sp) {
    Thread thread = null;
    if (sp.loadDynamicJar) {
      addPlantUmlJar(sp);
    }
    try {
      if (sp.showBrowser && Desktop.isDesktopSupported()) {
        thread = new Thread() {
          public void run() {
            try {
              synchronized (this) {
                this.wait(3000);
                if (sp.offset == 0) {
                  Desktop.getDesktop().browse(new URI("http://localhost:" + sp.port()));
                }
              }
            } catch (InterruptedException e) {
            } catch (Throwable t) {
              t.printStackTrace();
            }
          }
        };
        thread.start();
      }
      if (sp.offset < RETRIES) {
        System.out.println("pumlserver: listening on http://localhost:" + (sp.port()) + "/plantuml");
        new App(sp).listen(sp.port());
        return 0;
      }
      System.out.println("no port found after " + RETRIES + " tries");
      return -1;
    } catch (final RuntimeException ex) {
      try {
        if (thread != null) {
          thread.interrupt();
        }
      } catch (Throwable t) {
      }
      if ("Server start-up failed!".equals(ex.getMessage())) {
        final String urlPre = "http://localhost:" + (sp.port());
        try {
          if (ConnectionHelper.getLocalContent(urlPre + "/plantuml/txt/SoWkIImgAStDuN9KqBLJSE9oICrB0N81") != null) {

            System.out.println("Another PlantUML server is running on port " + sp.port() + " - stopping it!");
            // try to kill the other server
            if (ConnectionHelper.getLocalContent(urlPre + "/exit") != null) {
              // other server exited
              synchronized (AppStarter.class) {
                AppStarter.class.wait(500);
              }
              return startOnPort(sp.same());
            }
          }
          // continue with next port
          return startOnPort(sp.next());

        } catch (Throwable T) {
          T.printStackTrace();
        }
      }
      return -1;
    }
  }

  private static synchronized void loadLibrary(File jar, final AppParams sp) throws Throwable {
    ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    try {
      final Method method = classLoader.getClass().getDeclaredMethod("addURL", URL.class);
      method.setAccessible(true);
      method.invoke(classLoader, jar.toURI().toURL());
    } catch (Throwable t) {
      try {
        Method method = classLoader.getClass().getDeclaredMethod("appendToClassPathForInstrumentation", String.class);
        method.setAccessible(true);
        method.invoke(classLoader, jar.getAbsolutePath());
      } catch (Throwable T) {
        // TODO add custom classloader to manifest / pom.xml - this is just a (hopefully
        // temporary) workaround for java 14
        ArrayList<String> args = new ArrayList<String>();
        final String thisJar = new java.io.File(
            AppStarter.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getAbsolutePath();
        args.add("java");
        args.add("-cp");
        args.add(thisJar + File.pathSeparator + jar.getName());
        args.add(Main.class.getName());
        args.addAll(Arrays.asList(Main.theArgs));
        args.add("-j");
        System.out.println("java14 hack - restarting with: ");
        for (String arg : args) {
          System.out.print(arg);
          System.out.print(" ");
        }
        System.out.println();
        final String[] theArgs = new String[args.size()];
        Runtime.getRuntime().exec(args.toArray(theArgs), null, new File(".").getAbsoluteFile());
        System.out.println("Other job is running in background now - exiting.");
        System.out.println("pumlserver: listening on http://localhost:" + (sp.port()) + "/plantuml");
        System.exit(0);
      }
    }
  }

  private static boolean added = false;

  static void addPlantUmlJar(final AppParams sp) {
    if (added) {
      return;
    }
    String currentFile = null;
    final File[] files = new File(".").listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".jar") && name.startsWith("plantuml.");
      }
    });
    final boolean filesEmpty = files == null || files.length == 0;

    if (filesEmpty || sp.checkForUpdates) {
      try {
        currentFile = Download.getJar(Paths.get(""));
      } catch (Throwable T) {
        T.printStackTrace();
        System.err.println("No update done - no internet connection. Exiting...");
      }
    }

    if (sp.checkForUpdates) {
      CheckUpdates.checkUpdates();
    }

    if (currentFile != null) {
      if (files != null && files.length > 0) {
        // remove old files
        for (final File file : files) {
          if (!file.getName().equals(currentFile)) {
            file.delete();
          }
        }
      }
    } else if (!filesEmpty) {
      // use the newest of the files
      Arrays.sort(files);
      currentFile = files[files.length - 1].getName();
    }
    if (currentFile != null) {
      try {
        loadLibrary(new File(currentFile), sp);
        added = true;
      } catch (Throwable t) {
        t.printStackTrace();
      }
    }
  }

}
