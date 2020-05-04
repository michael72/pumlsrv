package com.github.michael72.pumlsrv;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;

/**
 * Check for updates on plantuml jar and download newest version.
 */
public class Download {
  private static final String root = "https://sourceforge.net/projects/plantuml";
  private static final String rss = root + "/rss?path=/";

  public static String getJar(final Path saveTo) throws IOException {
    System.out.println("Checking for updates");
    // check on RSS feed for newest available version
    final InputStream content = ConnectionHelper.getContent(rss);
    if (content != null) {
      final String lines = getContent(content);
      int idxJarEnd = lines.indexOf(".jar]") + 4;
      if (idxJarEnd != 3) {
        final int idxJar = lines.lastIndexOf("[", idxJarEnd) + 1;
        if (idxJar != 0) {
          final String jar = lines.substring(idxJar, idxJarEnd);
          final String filename = jar.substring(jar.lastIndexOf("/") + 1);
          if (Files.exists(Paths.get(saveTo + filename))) {
            System.out.println("Already got newest plantuml file " + filename);
            return filename;
          }
          // Download the jar file
          // sf sometimes is really slow - better download at mvnrepository
          String download = "https://repo1.maven.org/maven2/net/sourceforge/plantuml/plantuml"
              + jar.replace("plantuml.", "plantuml-");
          System.out.println("Downloading " + download + " ...");
          String result = null;
          try {
            result = downloadFile(new URL(download), filename, saveTo.toString());
          } catch (Throwable T) {
          }
          if (result == null) {
            download = root + "/files" + jar + "/download";
            System.out.println("Retrying download at " + download + " ...");
            result = downloadFile(new URL(download), filename, saveTo.toString());
          }
          return result;
        }
      }
    }
    return null;
  }

  private static final int BLOCK_SIZE = 4096;

  private static void writeTo(OutputStream out, InputStream in, Consumer<Integer> progress) throws IOException {
    int bytesRead = -1;
    final byte[] buf = new byte[BLOCK_SIZE];
    int readTotal = 0;

    while ((bytesRead = in.read(buf)) != -1) {
      out.write(buf, 0, bytesRead);
      if (progress != null) {
        readTotal += bytesRead;
        progress.accept(readTotal);
      }
    }
  }

  static String getContent(final InputStream content) throws IOException, UnsupportedEncodingException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    writeTo(bos, content, null);
    return new String(bos.toByteArray(), "UTF8");
  }

  private static String downloadFile(final URL url, String fileName, String saveDir) throws IOException {
    String result = null;

    final String path = new File(saveDir).getAbsolutePath() + File.separator + fileName;
    final File tmp = new File(path + "~");
    if (tmp.exists()) {
      tmp.delete();
    }

    final HttpURLConnection con = ConnectionHelper.getConnection(url);
    if (con.getResponseCode() == HttpURLConnection.HTTP_OK) {
      try (final InputStream in = con.getInputStream(); final FileOutputStream out = new FileOutputStream(tmp)) {
        // download file and print progress
        final int tick = 5; // print every 2%
        final int part = con.getContentLength() / (tick * 10); 
        final int[] next = new int[] { 1 };
        System.out.print("Progress %: 0");
        writeTo(out, in, (progress) -> {
          if (progress > next[0] * part) {
            System.out.print((next[0]++ % tick == 0) ? "" + next[0] / tick * 10 : ".");
          }
        });
        System.out.println();
      }
      tmp.renameTo(new File(path));
      result = fileName;

    } else {
      System.err.println("No file downloaded. Server replied HTTP code: " + con.getResponseCode());
    }
    con.disconnect();
    return result;
  }

}
