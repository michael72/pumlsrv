package com.github.michael72.pumlsrv;

import java.io.File;
import java.io.IOException;

import com.github.michael72.pumlsrv.AppParams.OutputMode;

public class MainHtml {
  final AppParams params;

  public MainHtml(final AppParams params) {
    this.params = params;
  }

  public String localhost() {
    return "http://localhost:" + this.params.port();
  }

  public String hello() {
    final StringBuilder buf = new StringBuilder("@startuml\ntitle:");
    buf.append("pumlsrv ").append(Resources.version).append(" running on port ").append(this.params.port());
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

  private void addToggle(final StringBuilder buf, final String label, final boolean checked, final String name) {
    final String href = "/" + name;
    buf.append("<td><label for=\"switch" + name + "\">" + label + "</label></td>");

    buf.append("<td><label class=\"switch\" id=\"switch" + name + "\">\n" + "<input type=\"checkbox\" "
        + (checked ? "checked" : "") + ">\n" + "<span class=\"slider round\" id=\"updateSwitch" + name + "\"></span>\n"
        + "</label></td>")
        .append("<script>" + "function onUpdate" + name + "() {\n" + "window.location.href='" + href + "';\n" + "}"
            + "function onUpdate2" + name + "() {\n" + "setTimeout(onUpdate" + name + ",400);\n" + "}"
            + "document.getElementById('updateSwitch" + name + "').onclick=onUpdate2" + name + ";\n" + "</script>");
  }

  public byte[] html() throws IOException {
    // create title
    final StringBuilder buf = new StringBuilder("<html><head>").append("<style>").append(Resources.switchCss)
        .append("html *\n" + "{\n" + "   font-family: Arial;\n" + "}\n").append("</style>").append("<title>pumlsrv ")
        .append(Resources.version).append(" configuration")
        .append("</title><link rel=\"shortcut icon\" href=\"" + localhost() + "/favicon.ico\">")
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
        addLink(buf, "color", "mono", "blue", "red", "yellow");
      } else {
        addLink(buf, "mono", "mono", "black", "gray", "white");
      }

      // format items in table
      buf.append("</p><table><tr>");
      // show port entry field
      buf.append("<form class=\"form-inline\" action=\"/move_to\">\n" + " <div class=\"form-group\"><td>\n"
          + "<label for=\"port\">Port:</label>\n" + "<input type=\"number\" id=\"port\" name=\"port\" value=\""
          + params.port() + "\">\n" + "</td></div><td>" + "<input type=\"submit\" value=\"Change\">\r\n" + "</td>\n"
          + "</form>");
      buf.append("</tr><tr>");
      // add toggle box for updates on start
      addToggle(buf, "Check for updates on start", params.checkForUpdates, "check_updates");
      buf.append("</tr><tr>");
      // add toggle box for opening browser on start
      addToggle(buf, "Open in browser on start", params.showBrowser, "show_browser");
      buf.append("</tr></table>").append("</p>");

      if (params.checkForUpdates) {
        final String updates = CheckUpdates.checkUpdates();
        if (updates.length() > 0) {
          buf.append("</p><p></hr>").append(updates);
          buf.append(
              "</p><p><a href=\"https://github.com/michael72/pumlsrv/releases/latest\">Download latest release</a>");
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
              "<p>Please check your internet connection and try again or download <a href=\"https://sourceforge.net/projects/plantuml/files\">here</a></p>");
      App.exitLater(200);
    }

    return buf.toString().getBytes();
  }
}
