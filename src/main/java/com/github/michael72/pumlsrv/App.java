package com.github.michael72.pumlsrv;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.rapidoid.buffer.Buf;
import org.rapidoid.bytes.BytesUtil;
import org.rapidoid.http.AbstractHttpServer;
import org.rapidoid.http.HttpStatus;
import org.rapidoid.http.MediaType;
import org.rapidoid.job.Jobs;
import org.rapidoid.net.Server;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.impl.RapidoidHelper;
import org.rapidoid.setup.Setup;

/// Simple implementation of HttpServer that handles Plantuml-requests 
/// both as get and post.
/// The plantuml specific requests are forwarded to PumlApp.
/// This App can also handle configuration requests that are sent from the main 
/// configuration HTML page.
public class App extends AbstractHttpServer {

  private final AppParams params;

  public App(final AppParams params) {
    this.params = params;
  }

  private static Map<String, MediaType> mediaTypes = Stream
      .of(new Object[][] { { "svg", MediaType.SVG }, { "png", MediaType.PNG },
          { "eps", MediaType.APPLICATION_POSTSCRIPT }, { "epstext", MediaType.TEXT_PLAIN },
          { "txt", MediaType.TEXT_PLAIN } })
      .collect(Collectors.toMap(data -> (String) data[0], data -> (MediaType) data[1]));

  private HttpStatus parsePost(final Channel ctx, final Buf buf, final RapidoidHelper req) throws IOException {
    ctx.write(fullResp(405, "HTTP method POST is not supported by this URL".getBytes()));
    return HttpStatus.DONE;
  }

  private boolean startsWith(final byte[] bytes, final Buf buf, final RapidoidHelper req) {
    return BytesUtil.startsWith(buf.bytes(), req.path, bytes, true);
  }

  private HttpStatus redirectToRoot(final Channel ctx) {
    // works better with scripting part than http-equiv + redirect (for IE)
    ctx.write(fullResp(200,
        ("<html><head><script>\n" + "window.location=\"/\"\n" + "</script></head><body/></html>").getBytes()));
    return HttpStatus.DONE;
  }

  private HttpStatus movePort(final int newPort, final Buf buf, final Channel ctx) {
    checkOldServer(true);
    params.setPort(newPort);
    this.listen(newPort);

    final String data = buf.data();
    boolean ie = false;
    final int idx = data.indexOf("User-Agent");
    if (idx != -1) {
      final String agent = data.substring(idx, data.indexOf('\n', idx));
      // ie: Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko
      ie = agent.contains("Trident");
    }

    final String url = "http://localhost:" + params.port();
    if (ie) {
      ctx.write(
          fullResp(200, ("<html><head><script>window.location=\"" + url + "\";</script></head></html>").getBytes()));
    } else {
      // non-ie browser works better with http-equiv when old location is not valid
      // any more
      ctx.write(fullResp(303,
          ("<html><head><meta http-equiv = \"refresh\" content=\"0; url=" + url + "\"/></head></html>").getBytes()));
    }
    return HttpStatus.DONE;
  }

  // All supported URL requests
  private static final byte PLANTUML[] = "/plantuml/".getBytes();
  private static final byte EXIT[] = "/exit".getBytes();
  private static final byte MONOCHROME[] = "/mono".getBytes();
  private static final byte DARK[] = "/dark".getBytes();
  private static final byte LIGHT[] = "/light".getBytes();
  private static final byte DEFAULT[] = "/default".getBytes();
  private static final byte MOVE_PORT[] = "/move_to".getBytes();
  private static final byte FAVICON[] = "/favicon.ico".getBytes();
  private static final byte ROOT[] = "/".getBytes();
  private static final byte ENABLE_CHECK[] = "/check_updates".getBytes();
  private static final byte SHOW_BROWSER[] = "/show_browser".getBytes();

  @Override
  protected HttpStatus handle(final Channel ctx, final Buf buf, final RapidoidHelper req) {
    try {
      if (req.isGet.value) {
        if (startsWith(PLANTUML, buf, req)) {
          // plantuml request
          // System.out.println(buf.asText()); // for debug purposes: print the actual HTML request
          final ParseUrl parseUrl = new ParseUrl(buf, req.path, PLANTUML.length);
          final ConverterResult conv_result = PumlApp.toImage(parseUrl, params);
          return ok(ctx, req.isKeepAlive.value, conv_result.bytes, mediaTypes.get(conv_result.image_type));
          // rest are config requests for this webserver
        } else if (startsWith(EXIT, buf, req)) {
          exitLater(50);
          return ok(ctx, req.isKeepAlive.value, "pumlsrv exiting...\nBYE!".getBytes(), mediaTypes.get("txt"));
        } else if (startsWith(MOVE_PORT, buf, req)) {
          final String port_str = BytesUtil.get(buf.bytes(), req.query);
          final int move_port = Integer.parseInt(port_str.substring(5)); // remove port=
          if (move_port != params.port()) {
            return movePort(move_port, buf, ctx);
          }
          return redirectToRoot(ctx);
        } else if (startsWith(MONOCHROME, buf, req)) {
          this.params.isMonoChrome = !this.params.isMonoChrome;
          return redirectToRoot(ctx);
        } else if (startsWith(DARK, buf, req)) {
          this.params.swapDarkMode();
          return redirectToRoot(ctx);
        } else if (startsWith(LIGHT, buf, req)) {
          this.params.swapLightMode();
          return redirectToRoot(ctx);
        } else if (startsWith(DEFAULT, buf, req)) {
          this.params.setDefaultMode();
          return redirectToRoot(ctx);
        } else if (startsWith(ENABLE_CHECK, buf, req)) {
          this.params.checkForUpdates = !this.params.checkForUpdates;
          return redirectToRoot(ctx);
        } else if (startsWith(SHOW_BROWSER, buf, req)) {
          this.params.showBrowser = !this.params.showBrowser;
          return redirectToRoot(ctx);
        } else if (startsWith(FAVICON, buf, req)) {
          return ok(ctx, req.isKeepAlive.value, Resources.favicon, MediaType.IMAGE_X_ICON);
        } else if (startsWith(ROOT, buf, req)) {
          checkOldServer(false);
          params.store();
          return ok(ctx, req.isKeepAlive.value, new MainHtml(params).html(), MediaType.HTML_UTF_8);
        } else {
          final String url = BytesUtil.get(buf.bytes(), req.path);
          System.err.println("URL not found: " + url);
          ctx.write(fullResp(404, "Page not found".getBytes()));
          return HttpStatus.NOT_FOUND;
        }
      } else {
        return parsePost(ctx, buf, req);
      }
    } catch (final IOException ex) {
      ex.printStackTrace();
      startResponse(ctx, req.isKeepAlive.value);
      final String body = "Error: could not parse UML code";
      writeBody(ctx, body.getBytes(), 0, body.length(), MediaType.TEXT_PLAIN);
      return HttpStatus.ERROR;
    }

  }

  private Server server;
  private Server oldServer;

  @Override
  public Server listen(int port) {
    this.server = super.listen(port);
    return this.server;
  }

  private void checkOldServer(final boolean shutdown) {
    if (oldServer != null) {
      synchronized (oldServer) {
        try {
          if (shutdown) {
            // back and forth and old server still running? wait a little...
            oldServer.wait(1000);
          } else {
            // notify shutdown thread that new server port is up
            oldServer.notify();
          }
        } catch (Throwable T) {
        }
      }
    }
    if (shutdown) {
      shutdownServer();
    }
  }

  private void shutdownServer() {
    oldServer = server;
    new Thread("ShutdownOldPort") {
      public void run() {
        synchronized (oldServer) {
          try {
            oldServer.wait(1000);
            oldServer.shutdown();
            oldServer = null;
          } catch (InterruptedException e) {
          }
        }
      }
    }.start();
  }

  public static void exitLater(int millis) {
    new Thread("ExitThread") {
      @Override
      public void run() {
        synchronized (App.class) {
          try {
            App.class.wait(millis);
          } catch (InterruptedException e) {
          }
          Jobs.shutdown();
          Setup.shutdownAll();
          System.exit(0);
        }
      }
    }.start();
  }
}