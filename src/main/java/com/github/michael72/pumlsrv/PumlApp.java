package com.github.michael72.pumlsrv;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/// Converts uml requests to the plantuml server to images.
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
    final int idxEnd = uml.lastIndexOf("@enduml");
    if (idx != -1) {
      if (idxEnd != -1) {
        uml = uml.substring(idx + startuml.length(), idxEnd);
      } else {
        uml = uml.substring(idx + startuml.length());
      }
    } else if (idxEnd != -1) {
      uml = uml.substring(0, idxEnd);
    }
    return uml;
  }

  public static ConverterResult toImage(final String uml_parts, final int idx, final AppParams params, String imageType)
      throws IOException {
    final StringBuilder umlBuf = new StringBuilder();
    final String newpage = "\nnewpage\n";
    final String[] parts = uml_parts.split(newpage);
    int partidx = -1;
    for (String uml : parts) {
      partidx++;
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
      if (umlBuf.length() > 0 || parts.length > 0) {
        uml = stripStartUml(uml);
      }
      umlBuf.append(uml);
      if (parts.length > 0 && partidx < parts.length - 1) {
        umlBuf.append(newpage);
      }
    }
    checkStartUml(umlBuf);

    ConverterResult result = UmlConverter.toImage(umlBuf.toString(), idx, imageType);
    if (result.isError) {
      umlBuf.setLength(0);
      umlBuf.append(uml_parts);
      checkStartUml(umlBuf);
      result = UmlConverter.toErrorResult(umlBuf.toString(), idx);
    }
    return result;
  }

  public static ConverterResult toImage(ParseUrl parseUrl, final AppParams params) throws IOException {
    return toImage(toUml(parseUrl.getContent()), parseUrl.getIndex(), params, parseUrl.getImageType());
  }

}