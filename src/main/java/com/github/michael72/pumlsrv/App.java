package com.github.michael72.pumlsrv;

import java.io.IOException;

import org.rapidoid.buffer.Buf;
import org.rapidoid.bytes.Bytes;
import org.rapidoid.bytes.BytesUtil;
import org.rapidoid.data.BufRange;
import org.rapidoid.http.AbstractHttpServer;
import org.rapidoid.http.HttpStatus;
import org.rapidoid.http.MediaType;
import org.rapidoid.net.abstracts.Channel;
import org.rapidoid.net.impl.RapidoidHelper;

/// Simple implementation of HttpServer that handles Plantuml-requests 
/// both as get and post - on the path /plantuml/svg creating an SVG
/// Image as a result.
public class App extends AbstractHttpServer {
  private static final int port = 8080;
  private static final byte PLANTUML[] = "/plantuml/".getBytes();

  
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
        return ok(ctx, req.isKeepAlive.value, toImage(parseUrl), MediaType.SVG);
      } else {
        return parsePost(ctx, buf, req);
      }
    } catch (final IOException ex) {
      ex.printStackTrace();
      startResponse(ctx, req.isKeepAlive.value);
      final String body = "Error: could not parse uml";
      writeBody(ctx, body.getBytes(), 0, body.length(), MediaType.TEXT_PLAIN);
      return HttpStatus.ERROR;
    }

  }

  public static void main(final String[] args) throws Exception {
    new App().listen(port);
  }
}