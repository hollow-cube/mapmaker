package net.hollowcube.anticheat.protocol;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/// 777 is 776 plus four packets and minus one, so the policy [Protocol776Test] pins down carries
/// over by name: what is checked here is the ids 26.3 shifted, the packets it added, and that every
/// shared packet is treated exactly as it is in 776.
class Protocol777Test {

    @Test
    void testTableSizesMatchTheRegistrationOrder() {
        assertEquals(69, Protocol777.PACKETS.entries(ProtocolState.PLAY, Direction.C2S).size());
        assertEquals(144, Protocol777.PACKETS.entries(ProtocolState.PLAY, Direction.S2C).size());
        assertEquals(10, Protocol777.PACKETS.entries(ProtocolState.CONFIGURATION, Direction.C2S).size());
        assertEquals(21, Protocol777.PACKETS.entries(ProtocolState.CONFIGURATION, Direction.S2C).size());
        assertEquals(1, Protocol777.PACKETS.entries(ProtocolState.HANDSHAKE, Direction.C2S).size());
        assertEquals(0, Protocol777.PACKETS.entries(ProtocolState.HANDSHAKE, Direction.S2C).size());
    }

    @Test
    void testKnownPacketIds() {
        assertEquals(0, playS2C("bundle_delimiter"));
        assertEquals(1, playS2C("add_entity"));
        assertEquals(35, playS2C("entity_position_sync"));
        assertEquals(37, playS2C("add_transient_block"));
        assertEquals(46, playS2C("level_chunk_with_light"));
        assertEquals(50, playS2C("login"));
        assertEquals(54, playS2C("move_entity_pos"));
        assertEquals(73, playS2C("player_position"));
        assertEquals(83, playS2C("post_effects"));
        assertEquals(84, playS2C("respawn"));
        assertEquals(120, playS2C("start_configuration"));
        assertEquals(123, playS2C("swing_animation"));
        assertEquals(143, playS2C("show_dialog"));

        assertEquals(13, playC2S("client_tick_end"));
        assertEquals(30, playC2S("move_player_pos"));
        assertEquals(33, playC2S("move_player_status_only"));
        assertEquals(45, playC2S("pong"));
        assertEquals(46, playC2S("punch"));
        assertEquals(-1, playC2S("swing"));
        assertEquals(7, Protocol777.PACKETS.packetId(ProtocolState.CONFIGURATION, Direction.S2C, "registry_data"));
        assertEquals(10, Protocol777.PACKETS.packetId(ProtocolState.CONFIGURATION, Direction.S2C, "post_effects"));
        assertEquals(3, Protocol777.PACKETS.packetId(ProtocolState.CONFIGURATION, Direction.C2S, "finish_configuration"));
    }

    @Test
    void testAddedPackets() {
        for (String name : List.of("add_transient_block", "post_effects", "swing_animation"))
            assertFalse(Protocol777.PACKETS.lookup(ProtocolState.PLAY, Direction.S2C, name).kept(), name);
        assertFalse(Protocol777.PACKETS.lookup(ProtocolState.CONFIGURATION, Direction.S2C, "post_effects").kept());

        var punch = Protocol777.PACKETS.lookup(ProtocolState.PLAY, Direction.C2S, "punch");
        assertEquals(PacketTable.KeepKind.KEEP, punch.kind());
    }

    /// Every packet both versions have is kept, decoded and fenced the same way, which is what
    /// lets the 776 policy tests stand for 777 too.
    @Test
    void testSharedPacketsKeepTheir776Treatment() {
        var added = Set.of("punch", "add_transient_block", "post_effects", "swing_animation");
        for (var state : ProtocolState.values()) {
            for (var direction : Direction.values()) {
                var seen = new HashSet<String>();
                for (var entry : Protocol777.PACKETS.entries(state, direction)) {
                    seen.add(entry.name());
                    var old = Protocol776.PACKETS.lookup(state, direction, entry.name());
                    if (old == PacketTable.UNKNOWN) {
                        assertTrue(added.contains(entry.name()), entry.name() + " is new in 777");
                        continue;
                    }
                    var where = state + " " + direction + " " + entry.name();
                    assertEquals(old.kind(), entry.kind(), where);
                    assertEquals(old.pingSet(), entry.pingSet(), where);
                    assertEquals(old.pingWhen(), entry.pingWhen(), where);
                    assertEquals(old.decoder() == null, entry.decoder() == null, where);
                }
                for (var entry : Protocol776.PACKETS.entries(state, direction))
                    if (!seen.contains(entry.name())) assertEquals("swing", entry.name());
            }
        }
    }

    /// The records whose layout 26.3 changed decode to their 777 shape.
    @Test
    void testChangedLayoutsDecodeTo777Records() {
        assertInstanceOf(S2CAnimate.V777.class, decode("animate", new ByteWriter().varInt(1).u8(0)));
        assertInstanceOf(S2CMoveEntityRot.V777.class,
            decode("move_entity_rot", new ByteWriter().varInt(1).bool(true).u8(0).u8(0)));
        assertInstanceOf(S2CEntityPositionSync.V777.class, decode("entity_position_sync",
            new ByteWriter().varInt(1).varInt(0).f64(0).f64(0).f64(0).f32(0).f32(0).bool(false)));
    }

    @Test
    void testWakeUpIsActionZero() {
        var when = Protocol777.PACKETS.lookup(ProtocolState.PLAY, Direction.S2C, "animate").pingWhen();
        assertNotNull(when);
        assertTrue(when.fence(new S2CAnimate.V777(9, S2CAnimate.V777.WAKE_UP), 7));
        // 2 was the wake-up in 776 and is the magic critical hit particle in 777.
        assertFalse(when.fence(new S2CAnimate.V777(9, 2), 7));
    }

    @Test
    void testLookupByIdAndNameAgree() {
        for (var state : ProtocolState.values()) {
            for (var direction : Direction.values()) {
                var entries = Protocol777.PACKETS.entries(state, direction);
                for (int id = 0; id < entries.size(); id++) {
                    var entry = entries.get(id);
                    assertSame(entry, Protocol777.PACKETS.lookup(state, direction, id));
                    assertEquals(id, Protocol777.PACKETS.packetId(state, direction, entry.name()));
                }
            }
        }
    }

    private static Packet decode(String name, ByteWriter body) {
        var decoder = Protocol777.PACKETS.lookup(ProtocolState.PLAY, Direction.S2C, name).decoder();
        assertNotNull(decoder, name);
        return decoder.decode(new ByteReader(body.toByteArray()));
    }

    private static int playS2C(String name) {
        return Protocol777.PACKETS.packetId(ProtocolState.PLAY, Direction.S2C, name);
    }

    private static int playC2S(String name) {
        return Protocol777.PACKETS.packetId(ProtocolState.PLAY, Direction.C2S, name);
    }
}
