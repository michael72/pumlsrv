package com.github.michael72.pumlsrv;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.rapidoid.buffer.Buf;
import org.rapidoid.bytes.BytesUtil;
import org.rapidoid.http.AbstractHttpServer;
import org.rapidoid.http.HttpStatus;
import org.rapidoid.http.MediaType;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.impl.RapidoidHelper;

/// Simple implementation of HttpServer that handles Plantuml-requests 
/// both as get and post - on the path /plantuml/svg creating an SVG
/// Image as a result.
public class App extends AbstractHttpServer {
  private static final int DEFAULT_PORT = 8080;
  private static final byte PLANTUML[] = "/plantuml/".getBytes();
  private static Map<String, MediaType> mediaTypes = Stream
      .of(new Object[][] { { "svg", MediaType.SVG }, { "png", MediaType.PNG },
          { "eps", MediaType.APPLICATION_POSTSCRIPT }, { "epstext", MediaType.TEXT_PLAIN },
          { "txt", MediaType.TEXT_PLAIN } })
      .collect(Collectors.toMap(data -> (String) data[0], data -> (MediaType) data[1]));

  private static String toUml(final String content) throws IOException {
    return UmlConverter.decode(content);
  }

  private static ConverterResult toImage(ParseUrl parseUrl) throws IOException {
    final String uml = toUml(parseUrl.getContent());
    return UmlConverter.toImage(uml, parseUrl.getImageType());
  }

  private HttpStatus parsePost(final Channel ctx, final Buf buf, final RapidoidHelper req) throws IOException {
    ctx.write(fullResp(405, "HTTP method POST is not supported by this URL".getBytes()));
    return HttpStatus.DONE;
  }

  @Override
  protected HttpStatus handle(final Channel ctx, final Buf buf, final RapidoidHelper req) {
    try {
      if (req.isGet.value && BytesUtil.startsWith(buf.bytes(), req.path, PLANTUML, true)) {
        final ParseUrl parseUrl = new ParseUrl(buf, req.path, PLANTUML.length);
        final ConverterResult conv_result = toImage(parseUrl);
        return ok(ctx, req.isKeepAlive.value, conv_result.bytes, mediaTypes.get(conv_result.image_type));
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

  static void startOnPort(final int port, int offset) {
    try {
      if (offset < 10) {
        System.out.println("pumlserver: listening on http://localhost:" + (port + offset) + "/plantuml");
        new App().listen(port + offset);
      }
    } catch (final RuntimeException ex) {
      if ("Server start-up failed!".equals(ex.getMessage())) {
        URL url;
        try {
          url = new URL("http://localhost:" + (port + offset) + "/plantuml/txt/SoWkIImgAStDuN9KqBLJSE9oICrB0N81");
          final HttpURLConnection con = (HttpURLConnection) url.openConnection();
          con.setRequestMethod("GET");
          try {
            final Object content = con.getContent();
            if (content != null) {
              System.out.println(content.toString());
            }
            System.out.println("Another PlantUML server is running on port " + (port + offset) + " - exiting!");
          }
          catch (IOException ioe) {
            // continue with next port
            startOnPort(port, offset + 1);
          }
        } catch (Throwable T) {
          T.printStackTrace();
        }
      }
    }
  }

  public static void main(final String[] args) throws Exception {

    int port = DEFAULT_PORT;
    if (args.length == 1) {
      port = Integer.parseInt(args[0]);
      System.out.println("Using port set in parameter: " + port);
    } else if (args.length != 0) {
      System.err.println("Usage: pumlserver <port>");
      System.exit(-1);
    } else {
      final String portEnv = System.getenv("PUMLSRV_PORT");
      if (portEnv != null) {
        port = Integer.parseInt(portEnv);
        System.out.println("Using port set in environment: " + port);
      }
    }
    
    startOnPort(port, 0);
  }
}