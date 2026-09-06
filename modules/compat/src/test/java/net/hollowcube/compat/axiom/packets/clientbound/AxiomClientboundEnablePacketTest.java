package net.hollowcube.compat.axiom.packets.clientbound;

import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static net.hollowcube.compat.axiom.Bytes.*;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AxiomClientboundEnablePacketTest {

    private static byte[] encode(AxiomClientboundEnablePacket.ServerConfig config) {
        return NetworkBuffer.makeArray(AxiomClientboundEnablePacket.TYPE.codec(), new AxiomClientboundEnablePacket(config));
    }

    @Test
    void disableIsASingleFalse() {
        assertArrayEquals(of(0x00), encode(null));
    }

    @Test
    void api9Layout() {
        var config = new AxiomClientboundEnablePacket.ServerConfig.V9(0x100000, 1, List.of(), List.of());
        assertArrayEquals(concat(
                of(0x01),                   // enabled
                of(0x00, 0x10, 0x00, 0x00), // Int setBufferMaxSize
                of(0x01),                   // VarInt blueprintVersion
                of(0x00),                   // blocksWithCustomData
                of(0x00)                    // ignoreRotationSet
        ), encode(config));
    }

    @Test
    void api10Layout() {
        var config = new AxiomClientboundEnablePacket.ServerConfig.V10(1, 31_000, 0x200000, List.of(), List.of(), List.of("axiom:hello", "axiom:tunnel"));
        assertArrayEquals(concat(
                of(0x01),                   // enabled
                of(0x00),                   // Byte config format version
                of(0x01),                   // VarInt blueprintVersion
                of(0x00, 0x00, 0x79, 0x18), // Int tunnelSplitSize
                of(0x00, 0x20, 0x00, 0x00), // Int maximumTunnelPacketSize
                of(0x00),                   // blocksWithCustomData
                of(0x00),                   // ignoreRotationSet
                of(0x02), string("axiom:hello"), string("axiom:tunnel")
        ), encode(config));
    }
}
