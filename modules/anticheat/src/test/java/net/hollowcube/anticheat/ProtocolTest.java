package net.hollowcube.anticheat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProtocolTest {

    @Test
    void testSupportedVersions() {
        assertTrue(Protocol.isSupported(Protocol.PVN_776));
        assertTrue(Protocol.isSupported(Protocol.PVN_777));
        assertFalse(Protocol.isSupported(775));
        assertFalse(Protocol.isSupported(778));
        assertEquals(Protocol.V777, Protocol.of(777));
        assertThrows(IllegalArgumentException.class, () -> Protocol.of(775));
    }
}
