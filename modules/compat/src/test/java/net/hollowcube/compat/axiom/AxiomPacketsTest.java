package net.hollowcube.compat.axiom;

import com.github.luben.zstd.Zstd;
import net.hollowcube.compat.axiom.packets.serverbound.AxiomServerboundEntityRequestPacket;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static net.hollowcube.compat.axiom.Bytes.*;
import static org.junit.jupiter.api.Assertions.*;

class AxiomPacketsTest {

    private static final UUID ID = new UUID(0x1122334455667788L, 0x99AABBCCDDEEFF00L);
    private static final byte[] ENTITY_REQUEST = concat(int64(77), varInt(1), int64(ID.getMostSignificantBits()), int64(ID.getLeastSignificantBits()));
    private static final AxiomServerboundEntityRequestPacket EXPECTED = new AxiomServerboundEntityRequestPacket(77, Set.of(ID));

    @Test
    void api10IsToldExactlyTheSupportedPackets() {
        assertEquals(Set.of(
                "axiom:hello", "axiom:tunnel",
                "axiom:set_block", "axiom:set_buffer", "axiom:spawn_entity", "axiom:manipulate_entity", "axiom:request_entity_data",
                "axiom:annotation_update", "axiom:delete_entity", "axiom:marker_nbt_request", "axiom:set_fly_speed",
                "axiom:set_gamemode", "axiom:set_world_property", "axiom:teleport"
        ), Set.copyOf(AxiomPackets.ADVERTISED));
        assertEquals(AxiomPackets.ADVERTISED.size(), Set.copyOf(AxiomPackets.ADVERTISED).size());
    }

    @Test
    void onlyTheFiveEditingPacketsMayBeTunneled() {
        var tunneled = AxiomPackets.SERVERBOUND.stream().filter(AxiomPackets.Definition::tunneled).map(AxiomPackets.Definition::id).toList();
        assertEquals(Set.of("axiom:set_block", "axiom:set_buffer", "axiom:spawn_entity", "axiom:manipulate_entity", "axiom:request_entity_data"), Set.copyOf(tunneled));
        assertNull(AxiomPackets.byTunnelChannel("axiom:tunnel"));
        assertNull(AxiomPackets.byTunnelChannel("axiom:request_chunk_data"));
        assertNull(AxiomPackets.byTunnelChannel("axiom:teleport"));
    }

    @Test
    void tunneledPayloadsMustBeFullyConsumed() throws Exception {
        var definition = AxiomPackets.byTunnelChannel("axiom:request_entity_data");
        assertNotNull(definition);
        assertEquals(EXPECTED, definition.decodeTunneled(ENTITY_REQUEST));
        assertThrows(AxiomTunnel.MalformedException.class, () -> definition.decodeTunneled(concat(ENTITY_REQUEST, of(0))));
        assertThrows(AxiomTunnel.MalformedException.class, () -> definition.decodeTunneled(int64(77)));
    }

    @Test
    void compressedFragmentsDecodeToTheOriginalPacket() throws Exception {
        var message = concat(string("axiom:request_entity_data"), int32(ENTITY_REQUEST.length), of(1), Zstd.compress(ENTITY_REQUEST));
        var tunnel = new AxiomTunnel();
        int half = message.length / 2;
        assertNull(tunnel.accept(concat(of(AxiomTunnel.FRAGMENT_FIRST), Arrays.copyOfRange(message, 0, half))));
        var assembled = tunnel.accept(concat(of(AxiomTunnel.FRAGMENT_LAST), Arrays.copyOfRange(message, half, message.length)));

        var decoded = AxiomTunnel.decode(assembled);
        assertArrayEquals(ENTITY_REQUEST, decoded.payload());
        assertEquals(EXPECTED, AxiomPackets.byTunnelChannel(decoded.channel()).decodeTunneled(decoded.payload()));
    }
}
