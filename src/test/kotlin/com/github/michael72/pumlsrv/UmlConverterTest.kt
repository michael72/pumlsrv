package com.github.michael72.pumlsrv

import java.io.IOException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test

class UmlConverterTest {

    companion object {
        const val HELLO_BOB_ENC = "SyfFKj2rKt3CoKnELR1Io4ZDoSa70000"
        const val HELLO_BOB = "@startuml\nBob -> Alice : hello\n@enduml"
    }

    @Test
    fun testEncodeUml() {
        try {
            val enc = UmlConverter.encode(HELLO_BOB)
            assertEquals(HELLO_BOB_ENC, enc)
        } catch (e: IOException) {
            fail("exception ${e}")
        }
    }

    @Test
    fun testDecodeUml() {
        try {
            val dec = UmlConverter.decode(HELLO_BOB_ENC)
            assertEquals(HELLO_BOB, dec)
        } catch (e: IOException) {
            fail("exception ${e}")
        }
    }
}
