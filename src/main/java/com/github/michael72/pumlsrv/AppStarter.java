package com.github.michael72.pumlsrv;

import java.awt.Desktop;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

public class AppStarter {
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
