package com.github.michael72.pumlsrv

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.net.InetAddress

class RemoteIncludeGuardTest {

    @Test
    fun blocksLoopbackLiteral() {
        assertFalse(RemoteIncludeGuard.isAllowed("http://127.0.0.1/x"))
        assertFalse(RemoteIncludeGuard.isAllowed("http://127.0.0.1:8080/exit"))
        assertFalse(RemoteIncludeGuard.isAllowed("http://[::1]/x"))
    }

    @Test
    fun blocksLocalhostName() {
        assertFalse(RemoteIncludeGuard.isAllowed("http://localhost/x"))
    }

    @Test
    fun blocksCloudMetadataEndpoint() {
        assertFalse(RemoteIncludeGuard.isAllowed("http://169.254.169.254/latest/meta-data/"))
    }

    @Test
    fun blocksPrivateRanges() {
        assertFalse(RemoteIncludeGuard.isAllowed("http://10.0.0.5/x"))
        assertFalse(RemoteIncludeGuard.isAllowed("http://172.16.0.1/x"))
        assertFalse(RemoteIncludeGuard.isAllowed("http://192.168.1.1/x"))
    }

    @Test
    fun blocksAnyLocalAndCarrierGradeNat() {
        assertFalse(RemoteIncludeGuard.isAllowed("http://0.0.0.0/x"))
        assertFalse(RemoteIncludeGuard.isAllowed("http://100.64.0.1/x"))
    }

    @Test
    fun blocksNonHttpSchemes() {
        assertFalse(RemoteIncludeGuard.isAllowed("file:///etc/passwd"))
        assertFalse(RemoteIncludeGuard.isAllowed("ftp://example.com/x"))
        assertFalse(RemoteIncludeGuard.isAllowed("gopher://example.com/x"))
    }

    @Test
    fun rejectsMalformedUrls() {
        assertFalse(RemoteIncludeGuard.isAllowed("not a url"))
        assertFalse(RemoteIncludeGuard.isAllowed(""))
        assertFalse(RemoteIncludeGuard.isAllowed("http://"))
    }

    @Test
    fun classifiesAddressesDirectly() {
        assertTrue(RemoteIncludeGuard.isBlocked(InetAddress.getByName("127.0.0.1")))
        assertTrue(RemoteIncludeGuard.isBlocked(InetAddress.getByName("169.254.169.254")))
        assertTrue(RemoteIncludeGuard.isBlocked(InetAddress.getByName("10.1.2.3")))
        assertTrue(RemoteIncludeGuard.isBlocked(InetAddress.getByName("::1")))
        assertTrue(RemoteIncludeGuard.isBlocked(InetAddress.getByName("fc00::1")))

        assertFalse(RemoteIncludeGuard.isBlocked(InetAddress.getByName("8.8.8.8")))
        assertFalse(RemoteIncludeGuard.isBlocked(InetAddress.getByName("1.1.1.1")))
        assertFalse(RemoteIncludeGuard.isBlocked(InetAddress.getByName("93.184.216.34")))
    }
}
