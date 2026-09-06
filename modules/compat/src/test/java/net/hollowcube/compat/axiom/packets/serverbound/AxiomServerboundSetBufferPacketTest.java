package net.hollowcube.compat.axiom.packets.serverbound;

import net.hollowcube.compat.axiom.data.buffers.AxiomBiomeBuffer;
import net.hollowcube.compat.axiom.data.buffers.AxiomBlockBuffer;
import net.kyori.adventure.key.Key;
import net.minestom.server.network.NetworkBuffer;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static net.hollowcube.compat.axiom.Bytes.*;
import static org.junit.jupiter.api.Assertions.*;

class AxiomServerboundSetBufferPacketTest {

    private static final UUID ID = new UUID(1, 2);
    private static final byte[] HEADER = concat(string("minecraft:overworld"), int64(1), int64(2));
    private static final byte[] END_OF_DATA = int64(0b1000000000000000000000000010000000000000000000000000100000000000L);

    @Test
    void blockBufferConsumesTheTrailingDispatchCount() {
        var bytes = concat(HEADER, of(0), END_OF_DATA, varInt(300));
        var buffer = NetworkBuffer.wrap(bytes, 0, bytes.length);
        var packet = buffer.read(AxiomServerboundSetBufferPacket.TYPE.codec());
        assertEquals("minecraft:overworld", packet.dimension());
        assertEquals(ID, packet.id());
        assertInstanceOf(AxiomBlockBuffer.class, packet.buffer());
        assertEquals(300, packet.clientAvailableDispatchSends());
        assertEquals(0, buffer.readableBytes());
    }

    @Test
    void biomeBufferDecodesPaletteAndCells() {
        var cells = new byte[4096];
        cells[1 + 2 * 16 + 3 * 256] = 1;
        cells[5] = 2;
        cells[6] = 3; // beyond the palette, ignored
        // chunk key (1, -1, 2) packed like a block position: x << 38 | (y & 0xFFF) | z << 12
        long key = (1L << 38) | 0xFFFL | (2L << 12);
        var bytes = concat(HEADER, of(1),
                of(2), string("minecraft:plains"), string("minecraft:desert"),
                of(0), int64(key), cells, END_OF_DATA, varInt(9));
        var buffer = NetworkBuffer.wrap(bytes, 0, bytes.length);
        var packet = buffer.read(AxiomServerboundSetBufferPacket.TYPE.codec());
        assertEquals(0, buffer.readableBytes());
        assertEquals(9, packet.clientAvailableDispatchSends());

        var biomes = assertInstanceOf(AxiomBiomeBuffer.class, packet.buffer());
        assertEquals(List.of(Key.key("minecraft:plains"), Key.key("minecraft:desert")), biomes.palette());
        var seen = new ArrayList<String>();
        biomes.forEach((x, y, z, biome) -> seen.add(x + "," + y + "," + z + "=" + biome.asString()));
        assertEquals(List.of("21,-16,32=minecraft:desert", "17,-14,35=minecraft:plains"), seen);
    }

    @Test
    void unknownBufferTypeIsRejected() {
        var bytes = concat(HEADER, of(2));
        assertThrows(IllegalArgumentException.class, () -> NetworkBuffer.wrap(bytes, 0, bytes.length).read(AxiomServerboundSetBufferPacket.TYPE.codec()));
    }
}
