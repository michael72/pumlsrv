package com.github.michael72.pumlsrv;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Check for updates of pumlsrv on github.
 */
public class CheckUpdates {

  private static String result = null;

  public static String checkUpdates() {
    if (result != null) {
      return result;
    }

    try {
      final String latest = "https://github.com/michael72/pumlsrv/releases/latest";
      final URL url = new URL(latest);
      final HttpURLConnection con = (HttpURLConnection) url.openConnection();
      con.setRequestMethod("GET");

      final Object content = con.getContent();
      if (content != null) {
        final String lines = Download.getContent((InputStream) content);
        int idx = lines.indexOf("Latest release");
        if (idx != -1) {
          idx = lines.indexOf("title=", idx);
          if (idx != -1) {
            idx = lines.indexOf('"', idx) + 1;
          }
        }
        if (idx > 0) {
          final String tagVersion = lines.substring(idx, lines.indexOf('"', idx));
          if (versionIsNewer(tagVersion)) {
            int idxDetails = lines.indexOf("<div class=\"markdown-body\">");
            final String details = lines.substring(idxDetails, lines.indexOf("<details", idxDetails));
            int idxTitle = lines.indexOf("class=\"release-header\"");
            idxTitle = lines.indexOf(tagVersion, idxTitle);
            idxTitle = lines.indexOf(">", idxTitle) + 1;
            final String title = lines.substring(idxTitle, lines.indexOf('<', idxTitle));
            result = "<h2>Release " + tagVersion + " available</h2>" + "<h3>" + title + "</h3>" + details;
            return result;
          }
        }
      }
    } catch (Throwable T) {
      T.printStackTrace();
    }

    result = "";
    return result;
  }

  private static int versionNum(String version) {
    version = version.trim();
    if (version.charAt(0) == 'v') {
      version = version.substring(1);
    }
    final String[] v = version.split("\\.");
    int ret = 0;
    for (int i = 0; i < 2; ++i) {
      ret = 10 * (ret + (i < v.length ? Integer.parseInt(v[i]) : 0));
    }
    return ret;
  }

  private static boolean versionIsNewer(final String checkVersion) {
    return versionNum(checkVersion) > versionNum(Resources.version);
  }

}
