package com.github.michael72.pumlsrv;

import org.rapidoid.buffer.Buf;
import org.rapidoid.data.BufRange;

public class ParseUrl {
  public ParseUrl(final Buf buf, final BufRange range, final int offset) {
    range.strip(offset, 0);
    final String[] urlParts = buf.get(range).split("/");
    this.imageType = urlParts[0];
    this.content = urlParts[urlParts.length-1].trim();
  }
  
  public String getImageType() {
    return imageType;
  }
  
  public String getContent() {
    return content;
  }
  
  private String imageType;
  private String content;

}
