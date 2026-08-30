package net.hollowcube.anticheat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtocolTest {

    @Test
    void testSupportedVersions() {
        assertTrue(Protocol.isSupported(Protocol.PVN_776));
        assertFalse(Protocol.isSupported(775));
    }
}
