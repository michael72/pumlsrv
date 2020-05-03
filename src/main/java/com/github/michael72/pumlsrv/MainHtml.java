package com.github.michael72.pumlsrv;

import java.io.File;
import java.io.IOException;

import com.github.michael72.pumlsrv.AppParams.OutputMode;

public class MainHtml {
  final AppParams params;

  public MainHtml(final AppParams params) {
    this.params = params;
  }

  public String hello() {
    final StringBuilder buf = new StringBuilder("@startuml\ntitle:");
    buf.append("pumlsrv ").append(Resources.version).append(" running on port ")
        .append(Integer.toString(this.params.port()));
    buf.append("\nAlice -> Bob: Hello pumlsrv!\n@enduml\n");
    return buf.toString();
  }

  private void addLink(final StringBuilder buf, final String name, final String href, final String bg, final String bc,
      final String fg) {
    final StringBuilder style = new StringBuilder("background-color: ").append(bg).append(";color: ").append(fg)
        .append(";margin-left: 6px; margin-right: 30px; margin-top: -6px; border-radius: 6px; border:2px solid ")
        .append(bc).append(";padding: 10px 10px;text-align: center;text-decoration: none;display: inline-block;");
    buf.append("<a style=\"").append(style).append("\" href=\"/").append(href).append("\">").append(name)
        .append("</a>");
  }

  private void addLink(final StringBuilder buf, final OutputMode mode, final String bg, final String bc,
      final String fg) {
    final String name = mode.toString().toLowerCase();
    addLink(buf, name, name, bg, bc, fg);
  }

  public byte[] html() throws IOException {
    final StringBuilder buf = new StringBuilder("<html><head>").append("<style>").append(Resources.switchCss)
        .append("html *\n" + "{\n" + "   font-family: Arial;\n" + "}\n").append("</style>").append("<title>pumlsrv ")
        .append(Resources.version).append("<link rel=\"shortcut icon\" href=\"/favicon.ico\">")
        .append("</title></head>");
    buf.append("<body>").append(Resources.pumlsrvSvg);

    try {
      // show hello UML diagram in current style setting
      buf.append("<p style=\"margin-top: -3px;\">").append(new String(PumlApp.toImage(hello(), params, "svg").bytes))
          .append("</p>");

      // mode selection
      buf.append("<p>");
      for (OutputMode mode : OutputMode.values()) {
        if (this.params.outputMode != mode) {
          switch (mode) {
          case Dark:
            addLink(buf, mode, "#203562", "#81D4FA", "white");
            break;
          case Light:
            addLink(buf, mode, "white", "#81D4FA", "black");
            break;
          case Default:
            addLink(buf, mode, "fffdcf", "#a00000", "black");
          }
        }
      }
      if (params.isMonoChrome) {
        addLink(buf, "color", "mono", "green", "red", "blue");
      } else {
        addLink(buf, "mono", "mono", "black", "gray", "white");
      }

      buf.append("</p><p>");
      buf.append("<form class=\"form-inline\" action=\"/move_to\">\n" + " <div class=\"form-group\">\n"
          + "  <label for=\"port\">Port:</label>\n" + "  <input type=\"text\" id=\"port\" name=\"port\" value=\""
          + params.port() + "\">\n" + "  <input type=\"submit\" value=\"Change\">\r\n" + "</div>\n" + "</form> ");
      buf.append("</p><p>");
      buf.append("<label for=\"switch\">Check for updates on start: </label>");
      buf.append("<label class=\"switch\" id=\"switch\">\n" + "<input type=\"checkbox\" "
          + (params.checkForUpdates ? "checked" : "") + ">\n"
          + "<span class=\"slider round\" id=\"updateSwitch\"></span>\n" + "</label>")
          .append("<script>" + "function onUpdate() {\n" + "window.location.href='/check_updates';\n" + "}"
              + "function onUpdate2() {\n" + "setTimeout(onUpdate,400);\n" + "}"
              + "document.getElementById('updateSwitch').onclick=onUpdate2;\n" + "</script>");

      if (params.checkForUpdates) {
        final String updates = CheckUpdates.checkUpdates();
        if (updates.length() > 0) {
          buf.append("</p><p></hr>").append(updates);
          buf.append("</p><p><a href=\"https://github.com/michael72/pumlsrv/releases/latest\">Download latest release</a>");
        }
      }

      buf.append("</p></body></html>");
    } catch (NoClassDefFoundError err) {
      // show the svg with a red color
      final String s = buf.toString().replaceAll("#006680", "#802020");
      buf.setLength(0);
      buf.append(s).append("<p><h2>Error: Unable to start pumlsrv!</h2><p>")
          .append("<p><b>No plantuml*.jar found in current directory and could not be downloaded from server</b></p>")
          .append("<p>Current directory is " + new File(".").getAbsolutePath() + "</p>").append(
              "<p>Please check your internet connection and try again or download <a href=\"https://sourceforge.net/projects/plantuml/files/latest\">here</a></p>");
      App.exitLater(200);
    }

    return buf.toString().getBytes();
  }
}
