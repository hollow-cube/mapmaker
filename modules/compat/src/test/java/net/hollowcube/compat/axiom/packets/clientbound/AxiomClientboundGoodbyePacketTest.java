package net.hollowcube.compat.axiom.packets.clientbound;

import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import static net.hollowcube.compat.axiom.Bytes.of;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class AxiomClientboundGoodbyePacketTest {

    @Test
    void reasonIsALengthPrefixedString() {
        var bytes = NetworkBuffer.makeArray(AxiomClientboundGoodbyePacket.TYPE.codec(), new AxiomClientboundGoodbyePacket("nope"));
        assertArrayEquals(of(0x04, 'n', 'o', 'p', 'e'), bytes);
    }
}
