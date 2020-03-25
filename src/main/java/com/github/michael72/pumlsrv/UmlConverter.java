package com.github.michael72.pumlsrv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.code.TranscoderUtil;

public class UmlConverter {

  /// Compress Plant-UML string with zlib.
  public static String encode(final String uml) throws IOException {
    return TranscoderUtil.getDefaultTranscoder().encode(uml.replaceAll("\r\n", "\n"));
  }

  /// Uncompress compressed string to the original Plant-UML string.
  public static String decode(final String source) throws IOException {

    // build the UML source from the compressed part of the URL
    final String text = TranscoderUtil.getDefaultTranscoder().decode(source);

    // encapsulate the UML syntax if necessary
    if (!text.startsWith("@")) {
      final StringBuilder plantUmlSource = new StringBuilder("@startuml\n").append(text);
      if (!text.endsWith("\n")) {
        plantUmlSource.append("\n");
      }
      return plantUmlSource.append("@enduml").toString();
    }
    return text;
  }

  /// Convert Plant-UML string to an SVG image.
  public static byte[] toSvg(final String uml) throws IOException {
    final SourceStringReader reader = new SourceStringReader(uml);
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    // Write the first image to "os"
    reader.outputImage(os, new FileFormatOption(FileFormat.SVG));
    os.close();
    // The XML is stored into svg
    final String svg = new String(os.toByteArray(), Charset.forName("UTF-8"));
    return svg.getBytes();
  }
}
