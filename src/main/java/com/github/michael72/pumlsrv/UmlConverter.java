package com.github.michael72.pumlsrv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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
  // supported formats are as in plantuml server:
  // png, svg, eps, epstext and txt.
  private static Map<String, FileFormat> fileFormats = Stream.of(new Object[][] {
    { "svg", FileFormat.SVG }, 
    { "png", FileFormat.PNG },
    { "eps", FileFormat.EPS },
    { "epstext", FileFormat.EPS_TEXT },
    { "txt", FileFormat.UTXT }
  }).collect(Collectors.toMap(data -> (String)data[0], data -> (FileFormat)data[1]));
  
  /// Convert Plant-UML string to an image.
  public static byte[] toImage(final String uml, final String image_type) throws IOException {
    final SourceStringReader reader = new SourceStringReader(uml);
    final ByteArrayOutputStream os = new ByteArrayOutputStream();
    
    // Write the first image to "os"
    reader.outputImage(os, new FileFormatOption(fileFormats.get(image_type)));
    os.close();

    final String img = new String(os.toByteArray(), Charset.forName("UTF-8"));
    return img.getBytes();
  }

}
