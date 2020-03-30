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

  private static byte[] toImage(ParseUrl parseUrl) throws IOException {
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
        return ok(ctx, req.isKeepAlive.value, toImage(parseUrl), mediaTypes.get(parseUrl.getImageType()));
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

  public static void main(final String[] args) throws Exception {
    int port = DEFAULT_PORT;
    if (args.length == 1) {
      port = Integer.parseInt(args[0]);
    } else if (args.length != 0) {
      System.err.println("Usage: pumlserver <port>");
      System.exit(-1);

    }
    System.out.println("pumlserver: listening on http://localhost:" + port + "/plantuml");
    new App().listen(port);
  }
}