package com.github.michael72.pumlsrv;

import java.awt.Desktop;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/// Simple implementation of HttpServer that handles Plantuml-requests 
/// both as get and post - on the path /plantuml/svg creating an SVG
/// Image as a result.
public class PumlApp {

  private static String toUml(final String content) throws IOException {
    return UmlConverter.decode(content);
  }

  private static void checkStartUml(final StringBuilder umlBuf) {
    // encapsulate the UML syntax if necessary
    if (umlBuf.charAt(0) != '@') {
      umlBuf.insert(0, "@startuml\n");
      if (umlBuf.charAt(umlBuf.length() - 1) != '\n') {
        umlBuf.append("\n");
      }
      umlBuf.append("@enduml\n");
    }
  }

  final static String startuml = "@startuml";

  private static String stripStartUml(String uml) {
    final int idx = uml.indexOf(startuml);
    if (idx != -1) {
      final int idxEnd = uml.lastIndexOf("@enduml");
      if (idxEnd != -1) {
        uml = uml.substring(idx + startuml.length(), idxEnd);
      }
    }
    return uml;
  }

  public static ConverterResult toImage(String uml, final AppParams params, String imageType) throws IOException {
    final StringBuilder umlBuf = new StringBuilder();
    switch (params.outputMode) {
    case Dark:
      umlBuf.append(Style.darkTheme());
      break;
    case Light:
      umlBuf.append(Style.lightTheme());
      break;
    case Default:
      break;
    }
    if (params.includeFile != null) {
      if (params.reload) {
        try {
          String inc = new String(Files.readAllBytes(params.includeFile.toPath()), StandardCharsets.US_ASCII);
          umlBuf.append(stripStartUml(inc));
        } catch (IOException e) {
          e.printStackTrace();
        }
      } else {
        umlBuf.append("!include " + params.includeFile.getAbsolutePath());
      }
      umlBuf.append("\n");
    }
    if (params.isMonoChrome) {
      umlBuf.append("skinparam monochrome true\n");
    }
    if (umlBuf.length() > 0) {
      uml = stripStartUml(uml);
    }
    umlBuf.append(uml);
    checkStartUml(umlBuf);
    return UmlConverter.toImage(umlBuf.toString(), imageType);
  }

  public static ConverterResult toImage(ParseUrl parseUrl, final AppParams params) throws IOException {
    return toImage(toUml(parseUrl.getContent()), params, parseUrl.getImageType());
  }

  private static final int RETRIES = 10;

  static int startOnPort(final AppParams sp) {
    Thread thread = null;

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
        URL url;
        try {
          final String urlPre = "http://localhost:" + (sp.port());
          url = new URL(urlPre + "/plantuml/txt/SoWkIImgAStDuN9KqBLJSE9oICrB0N81");
          final HttpURLConnection con = (HttpURLConnection) url.openConnection();
          con.setRequestMethod("GET");
          try {
            final Object content = con.getContent();
            if (content != null) {
              System.out.println(content.toString());
            }
            System.out.println("Another PlantUML server is running on port " + sp.port() + " - stopping it!");
            // try to kill the other server
            url = new URL(urlPre + "/exit");
            final HttpURLConnection conExit = (HttpURLConnection) url.openConnection();
            conExit.setRequestMethod("GET");
            try {
              conExit.getContent();
            } catch (java.net.ConnectException ce) {
            }
            synchronized (con) {
              con.wait(200);
            }
            return startOnPort(sp.same());
          } catch (IOException ioe) {
            ioe.printStackTrace();
            // continue with next port
            return startOnPort(sp.next());
          }
        } catch (Throwable T) {
          T.printStackTrace();
        }
      }
      return -1;
    }
  }

}