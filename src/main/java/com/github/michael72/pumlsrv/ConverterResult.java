package com.github.michael72.pumlsrv;

public class ConverterResult {
  public ConverterResult(final byte[] bytes, final String description, final String image_type, final boolean isError) {
    this.bytes = bytes;
    this.description = description;
    this.image_type = image_type;
    this.isError = isError;
  }

  public final byte[] bytes;
  public final String description;
  public final String image_type;
  public final boolean isError;
}
