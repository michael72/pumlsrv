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
  private static final byte PLANTUML_SVG[] = "/plantuml/svg/".getBytes();

  private static String toUml(final Buf buf, final BufRange range) throws IOException {
    // currently only svg supported
    range.strip(PLANTUML_SVG.length, 0);
    final Bytes bytes = buf.bytes();
    int idx = range.start + 1;
    while (bytes.get(idx) != (byte) '/' && idx < range.last()) {
      ++idx;
    }
    if (idx < range.last()) {
      range.set(idx + 1, range.length - (idx - range.start));
    }
    final String zipped = buf.get(range);

    return UmlConverter.decode(zipped);
  }

  private static byte[] toSvg(final Buf buf, final BufRange range) throws IOException {
    final String uml = toUml(buf, range);
    return UmlConverter.toSvg(uml);
  }

  private static final boolean support_post = false;

  private HttpStatus parsePost(final Channel ctx, final Buf buf, final RapidoidHelper req) throws IOException {
    if (support_post) {
      final String msg = buf.asText();
      final int idx = msg.indexOf("@startuml");
      final int idxEnd = msg.lastIndexOf("@enduml");
      if (idx != -1 && idxEnd != -1) {
        final String uml = msg.substring(idx, idxEnd + 7);
        return ok(ctx, req.isKeepAlive.value, UmlConverter.toSvg(uml), MediaType.SVG);
      }
      return HttpStatus.NOT_FOUND;
    } else {
      ctx.write(fullResp(405, "HTTP method POST is not supported by this URL".getBytes()));
      return HttpStatus.DONE;
    }
  }

  @Override
  protected HttpStatus handle(final Channel ctx, final Buf buf, final RapidoidHelper req) {
    try {
      if (req.isGet.value && BytesUtil.startsWith(buf.bytes(), req.path, PLANTUML_SVG, true)) {
        return ok(ctx, req.isKeepAlive.value, toSvg(buf, req.path), MediaType.SVG);
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