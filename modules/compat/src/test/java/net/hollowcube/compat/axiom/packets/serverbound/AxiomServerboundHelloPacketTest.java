package net.hollowcube.compat.axiom.packets.serverbound;

import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import static net.hollowcube.compat.axiom.Bytes.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class AxiomServerboundHelloPacketTest {

    private static final byte[] DATA_VERSION_4900 = of(0xA4, 0x26);
    private static final byte[] PROTOCOL_776 = of(0x88, 0x06);

    @Test
    void api9HelloHasThreeVarInts() {
        var buffer = NetworkBuffer.wrap(concat(of(0x09), DATA_VERSION_4900, PROTOCOL_776), 0, 5);
        var packet = buffer.read(AxiomServerboundHelloPacket.TYPE.codec());
        assertEquals(new AxiomServerboundHelloPacket(9, 4900, 776, 0), packet);
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void api10HelloAppendsHandshakeId() {
        var bytes = concat(of(0x0A), DATA_VERSION_4900, PROTOCOL_776, of(0x01, 0x23, 0x45, 0x67, 0x89, 0xAB, 0xCD, 0xEF));
        var buffer = NetworkBuffer.wrap(bytes, 0, bytes.length);
        var packet = buffer.read(AxiomServerboundHelloPacket.TYPE.codec());
        assertEquals(new AxiomServerboundHelloPacket(10, 4900, 776, 0x0123456789ABCDEFL), packet);
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void unknownApiIgnoresTheRestOfThePayload() {
        var bytes = concat(of(0x0B), repeat(17, 3));
        var buffer = NetworkBuffer.wrap(bytes, 0, bytes.length);
        var packet = buffer.read(AxiomServerboundHelloPacket.TYPE.codec());
        assertEquals(new AxiomServerboundHelloPacket(11, 0, 0, 0), packet);
        assertEquals(0, buffer.readableBytes());
    }
}
