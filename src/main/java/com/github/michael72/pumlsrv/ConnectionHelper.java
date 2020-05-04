package com.github.michael72.pumlsrv;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

/**
 * Helper class to open a URL connection with a configured proxy setting.
 */
public class ConnectionHelper {
  
  private static HttpURLConnection getConnection(final URL url, boolean useProxy) throws IOException {
    final String http_proxy = System.getenv("HTTP_PROXY");
    String proxySetting = url.getProtocol().toLowerCase().contains("https") ? System.getenv("HTTPS_PROXY") : http_proxy;
    if (useProxy && proxySetting == null && http_proxy != null) {
      proxySetting = http_proxy; // http is fallback for https
    }
    if (useProxy && proxySetting != null && proxySetting.contains(":")) {
      final int idx = proxySetting.lastIndexOf(':');
      Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(
          // ip and port
          proxySetting.substring(proxySetting.lastIndexOf('/', idx) + 1, idx),
          Integer.parseInt(proxySetting.substring(idx + 1))));
      return (HttpURLConnection) url.openConnection(proxy);
    } else {
      return (HttpURLConnection) url.openConnection();
    }
  }
  
  public static HttpURLConnection getConnection(final URL url) throws IOException {
    return getConnection(url, true);
  }

  public static HttpURLConnection getLocalConnection(final URL url) throws IOException {
    return getConnection(url, false);
  }
  

  private static InputStream getContent(final String httpUrl, final boolean useProxy) {
    try {
      final URL url = new URL(httpUrl);
      final HttpURLConnection con = getConnection(url, useProxy);
      return (InputStream) con.getContent();
    } catch (IOException ioe) {
      return null;
    }
  }
  
  public static InputStream getLocalContent(final String httpUrl) {
    return getContent(httpUrl, false);
  }
  
  public static InputStream getContent(final String httpUrl) {
    return getContent(httpUrl, true);
  }
  
  
}
