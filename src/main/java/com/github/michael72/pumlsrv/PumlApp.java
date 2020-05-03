package com.github.michael72.pumlsrv;

import java.io.IOException;
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

}