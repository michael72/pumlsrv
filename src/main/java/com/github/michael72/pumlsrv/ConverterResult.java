package com.github.michael72.pumlsrv;

public class ConverterResult {
  public ConverterResult(final byte[] bytes, final String description, final String image_type) {
    this.bytes = bytes;
    this.description = description;
    this.image_type = image_type;
  }

  public final byte[] bytes;
  public final String description;
  public final String image_type;
}
