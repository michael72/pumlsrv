package com.github.michael72.pumlsrv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.text.StringEscapeUtils;

import net.sourceforge.plantuml.FileFormat;
import net.sourceforge.plantuml.FileFormatOption;
import net.sourceforge.plantuml.SourceStringReader;
import net.sourceforge.plantuml.code.TranscoderUtil;
import net.sourceforge.plantuml.core.DiagramDescription;

public class UmlConverter {

  /// Compress Plant-UML string with zlib.
  public static String encode(final String uml) throws IOException {
    return TranscoderUtil.getDefaultTranscoder().encode(uml.replaceAll("\r\n", "\n"));
  }

  /// Uncompress compressed string to the original Plant-UML string.
  public static String decode(final String source) throws IOException {

    // build the UML source from the compressed part of the URL
    return TranscoderUtil.getDefaultTranscoder().decode(source);
  }

  // supported formats are as in plantuml server:
  // png, svg, eps, epstext and txt.
  private static Map<String, FileFormat> fileFormats = Stream
      .of(new Object[][] { { "svg", FileFormat.SVG }, { "png", FileFormat.PNG }, { "eps", FileFormat.EPS },
          { "epstext", FileFormat.EPS_TEXT }, { "txt", FileFormat.UTXT } })
      .collect(Collectors.toMap(data -> (String) data[0], data -> (FileFormat) data[1]));

  
  
  /// Convert Plant-UML string to an image.
  public static ConverterResult toImage(final String uml, final String image_type) throws IOException {
    final SourceStringReader reader = new SourceStringReader(uml);
    ByteArrayOutputStream os = new ByteArrayOutputStream();

    // Write the first image to "os"
    final DiagramDescription desc = reader.outputImage(os, new FileFormatOption(fileFormats.get(image_type)));
    String img;

    if (desc.getDescription() == "(Error)" && image_type.contentEquals("svg")) {
      // re-write the SVG output with the actual text content
      // otherwise there is too much noise in the created image
      os = new ByteArrayOutputStream();
      reader.outputImage(os, new FileFormatOption(fileFormats.get("txt")));
      img = new String(os.toByteArray(), Charset.forName("UTF-8"));
 
      final String[] lines = img.split("\n");
      for (int i = 0; i < lines.length; ++i) {
        lines[i] = StringEscapeUtils.escapeHtml4(lines[i]);
      }

      int y = 20;
      final int line_height = 25;
      final int height = y + lines.length * line_height;

      img = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
          + "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" contentScriptType=\"application/ecmascript\" contentStyleType=\"text/css\" version=\"1.1\" zoomAndPan=\"magnify\" "
          + "preserveAspectRatio=\"none\" height=\"" + Integer.toString(height) + "\" width=\"200\">\n"
          + "<text x=\"0\" y=\"20\" fill=\"red\">" + lines[0] + "\n";

      for (int i = 1; i < lines.length; ++i) {
        y += line_height;
        img += "<tspan x=\"10\" y=\"" + Integer.toString(y) + "\">" + lines[i] + "</tspan>\n";
      }
      img += "</text>\n</svg>";
    }

    else {
      img = new String(os.toByteArray(), Charset.forName("UTF-8"));
    }

    return new ConverterResult(img.getBytes(), desc.getDescription(), image_type);
  }

}
