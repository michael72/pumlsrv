package com.github.michael72.pumlsrv;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;

class UmlConverterTest {

  static String HELLO_BOB_ENC = "SyfFKj2rKt3CoKnELR1Io4ZDoSa70000";
  static String HELLO_BOB = "@startuml\n" + "Bob -> Alice : hello\n" + "@enduml";

  @Test
  void testEncodeUml() {
    try {
      final String enc = UmlConverter.encode(HELLO_BOB);
      assertEquals(HELLO_BOB_ENC, enc);
    } catch (IOException e) {
      fail("exception " + e.toString());
    }
  }

  @Test
  void testDecodeUml() {
    try {
      final String dec = UmlConverter.decode(HELLO_BOB_ENC);
      assertEquals(HELLO_BOB, dec);
    } catch (IOException e) {
      fail("exception " + e.toString());
    }
  }

}
