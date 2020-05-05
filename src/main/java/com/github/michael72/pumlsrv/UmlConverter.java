package com.github.michael72.pumlsrv;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
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
  public static ConverterResult toImage(final String uml, final String imageType) throws IOException {
    final ByteArrayOutputStream os = new ByteArrayOutputStream();

    // Write the first image to "os"
    final DiagramDescription desc = new SourceStringReader(uml).outputImage(os,
        new FileFormatOption(fileFormats.get(imageType)));
    final boolean isError = desc.getDescription() == "(Error)" && imageType.contentEquals("svg");

    return new ConverterResult(os.toByteArray(), desc.getDescription(), imageType, isError);
  }

  public static ConverterResult toErrorResult(final String uml) throws IOException {
    // re-write the SVG output with the actual text content
    // otherwise there is too much noise in the created image
    final ConverterResult result = toImage(uml, "txt");
    String img = new String(result.bytes, Charset.forName("UTF-8"));

    int max_length = 0;
    final String[] arr = img.split("\n");
    List<String> lines = new ArrayList<String>(arr.length);
    int idx = 0;
    while (idx < arr.length) {
      String line = arr[idx].trim();
      if (line.length() > 0 && line.charAt(0) != '@') {
        // trim right and escape to HTML
        line = StringEscapeUtils.escapeHtml4(arr[idx].replaceAll("\\s+$", ""));
        max_length = Math.max(max_length, line.length());
        lines.add(line);
      }
      ++idx;
    }
    if (lines.size() > 8) {
      final List<String> croppedList = lines.subList(0, 4);
      croppedList.addAll(lines.subList(lines.size() - 4, lines.size()));
      lines = croppedList;
    }

    final int y_offset = 20;
    int y = y_offset;
    final int line_height = 25;
    final int char_width = 8;
    final int height = y + lines.size() * line_height;
    final int width = max_length * char_width;

    img = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>"
        + "<svg xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" contentScriptType=\"application/ecmascript\" contentStyleType=\"text/css\" version=\"1.1\" zoomAndPan=\"magnify\" "
        + "preserveAspectRatio=\"none\" height=\"" + height + "\" width=\"" + width + "\">\n" + "<text x=\"0\" y=\""
        + y_offset + "\" fill=\"red\">" + lines.get(0) + "\n";

    for (int i = 1; i < lines.size(); ++i) {
      y += line_height;
      img += "<tspan x=\"10\" y=\"" + Integer.toString(y) + "\">" + lines.get(i) + "</tspan>\n";
    }
    img += "</text>\n</svg>";

    return new ConverterResult(img.getBytes(), result.description, "svg", false);
  }

}
