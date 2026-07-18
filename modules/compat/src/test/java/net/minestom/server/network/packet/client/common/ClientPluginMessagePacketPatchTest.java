package net.minestom.server.network.packet.client.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

// Guards the patched copy of ClientPluginMessagePacket in this module (which shadows the Minestom
// jar's class). Note this only proves the patch works on compat's classpath — each server binary's
// shadow jar is separately verified by the verifyMinestomPatches task in mapmaker.java-binary.
class ClientPluginMessagePacketPatchTest {

    @Test
    void acceptsDataAboveVanillaCap() {
        assertDoesNotThrow(() -> new ClientPluginMessagePacket("axiom:set_buffer", new byte[Short.MAX_VALUE + 1]));
        assertDoesNotThrow(() -> new ClientPluginMessagePacket("axiom:set_buffer", new byte[0x100000]));
    }

    @Test
    void rejectsOverlongChannel() {
        assertThrows(IllegalArgumentException.class, () -> new ClientPluginMessagePacket("x".repeat(257), new byte[0]));
    }
}
