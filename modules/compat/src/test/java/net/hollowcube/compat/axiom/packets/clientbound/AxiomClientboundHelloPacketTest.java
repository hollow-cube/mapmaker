package net.hollowcube.compat.axiom.packets.clientbound;

import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import static net.hollowcube.compat.axiom.Bytes.of;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AxiomClientboundHelloPacketTest {

    @Test
    void handshakeIdIsABigEndianLong() {
        var bytes = NetworkBuffer.makeArray(AxiomClientboundHelloPacket.TYPE.codec(), new AxiomClientboundHelloPacket(0x0123456789ABCDEFL));
        assertArrayEquals(of(0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF), bytes);
    }
}
