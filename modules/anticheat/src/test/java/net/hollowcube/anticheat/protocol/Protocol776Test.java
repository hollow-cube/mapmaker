package net.hollowcube.anticheat.protocol;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.junit.jupiter.api.Assertions.*;

class Protocol776Test {

    /// Every S2C row `anticheat-packet-audit.md` marks YES, in play-phase id order, including the
    /// common packets that are registered into the play protocol.
    private static final List<String> AUDIT_YES = List.of(
        "add_entity", "animate", "block_changed_ack", "block_event", "block_update", "clear_dialog",
        "container_close", "container_set_content", "container_set_slot", "cooldown", "damage_event",
        "entity_event", "entity_position_sync", "explode", "forget_level_chunk", "game_event",
        "initialize_border", "level_chunk_with_light", "login", "mount_screen_open", "move_entity_pos",
        "move_entity_pos_rot", "move_entity_rot", "move_minecart_along_track", "move_vehicle", "open_book",
        "open_screen", "open_sign_editor", "player_abilities", "player_combat_kill", "player_info_update",
        "player_look_at", "player_position", "player_rotation", "remove_entities", "remove_mob_effect",
        "resource_pack_push", "respawn", "section_blocks_update", "set_border_center", "set_border_lerp_size",
        "set_border_size", "set_camera", "set_chunk_cache_center", "set_chunk_cache_radius", "set_entity_data",
        "set_entity_motion", "set_equipment", "set_health", "set_held_slot", "set_passengers",
        "set_player_inventory", "set_player_team", "set_time", "show_dialog", "teleport_entity",
        "ticking_state", "ticking_step", "update_attributes", "update_mob_effect", "update_tags");

    /// Packets whose effect lands on one tracked entity. Pinging after each of these would ping
    /// after nearly every frame, and the reader can bracket them with the surrounding pings.
    private static final Set<String> ENTITY_GROUP = Set.of(
        "add_entity", "animate", "damage_event", "entity_event", "entity_position_sync", "move_entity_pos",
        "move_entity_pos_rot", "move_entity_rot", "move_minecart_along_track", "move_vehicle", "remove_entities",
        "remove_mob_effect", "set_entity_data", "set_entity_motion", "set_equipment", "set_passengers",
        "teleport_entity", "update_attributes", "update_mob_effect");

    /// Packets that open (or close) a screen. Their effect on movement is "input goes to zero",
    /// which the reader sees from the input stream itself.
    private static final Set<String> SCREEN_GROUP = Set.of(
        "clear_dialog", "container_close", "mount_screen_open", "open_book", "open_screen", "open_sign_editor",
        "player_combat_kill", "resource_pack_push", "show_dialog");

    @Test
    void testTableSizesMatchTheRegistrationOrder() {
        assertEquals(69, Protocol776.table(ProtocolState.PLAY, Direction.C2S).size());
        assertEquals(141, Protocol776.table(ProtocolState.PLAY, Direction.S2C).size());
        assertEquals(10, Protocol776.table(ProtocolState.CONFIGURATION, Direction.C2S).size());
        assertEquals(20, Protocol776.table(ProtocolState.CONFIGURATION, Direction.S2C).size());
        assertEquals(1, Protocol776.table(ProtocolState.HANDSHAKE, Direction.C2S).size());
        assertEquals(0, Protocol776.table(ProtocolState.HANDSHAKE, Direction.S2C).size());
    }

    @Test
    void testKnownPacketIds() {
        assertEquals(0, playS2C("bundle_delimiter"));
        assertEquals(1, playS2C("add_entity"));
        assertEquals(45, playS2C("level_chunk_with_light"));
        assertEquals(49, playS2C("login"));
        assertEquals(72, playS2C("player_position"));
        assertEquals(118, playS2C("start_configuration"));
        assertEquals(140, playS2C("show_dialog"));

        assertEquals(30, Protocol776.packetId(ProtocolState.PLAY, Direction.C2S, "move_player_pos"));
        assertEquals(33, Protocol776.packetId(ProtocolState.PLAY, Direction.C2S, "move_player_status_only"));
        assertEquals(45, Protocol776.packetId(ProtocolState.PLAY, Direction.C2S, "pong"));
        assertEquals(7, Protocol776.packetId(ProtocolState.CONFIGURATION, Direction.S2C, "registry_data"));
        assertEquals(3, Protocol776.packetId(ProtocolState.CONFIGURATION, Direction.C2S, "finish_configuration"));
    }

    @Test
    void testDecodedPacketsHaveDecodersAndKeepListPacketsAreKept() {
        for (String name : List.of("level_chunk_with_light", "block_update", "section_blocks_update",
            "forget_level_chunk", "set_chunk_cache_center", "set_chunk_cache_radius", "login", "respawn",
            "start_configuration", "add_entity", "teleport_entity", "entity_position_sync", "move_entity_pos",
            "remove_entities", "set_passengers", "player_position", "player_rotation", "ping", "custom_payload",
            "update_tags", "update_mob_effect", "remove_mob_effect", "set_entity_data", "set_equipment",
            "container_set_slot", "container_set_content", "animate", "entity_event", "set_entity_motion")) {
            var entry = Protocol776.lookup(ProtocolState.PLAY, Direction.S2C, name);
            assertEquals(Protocol776.KeepKind.KEEP_DECODE, entry.kind(), name);
            assertNotNull(entry.decoder(), name);
        }

        for (String name : List.of("move_player_pos", "move_player_pos_rot", "move_player_rot",
            "move_player_status_only", "pong", "configuration_acknowledged", "custom_payload")) {
            var entry = Protocol776.lookup(ProtocolState.PLAY, Direction.C2S, name);
            assertEquals(Protocol776.KeepKind.KEEP_DECODE, entry.kind(), name);
            assertNotNull(entry.decoder(), name);
        }

        // Every audit YES row is at least kept, whether or not it is decoded.
        for (String name : AUDIT_YES)
            assertTrue(Protocol776.lookup(ProtocolState.PLAY, Direction.S2C, name).kept(), name);

        // C2S inputs the simulator replays.
        for (String name : List.of("accept_teleportation", "attack", "interact", "player_action", "player_command",
            "player_input", "use_item", "use_item_on", "set_carried_item", "client_tick_end", "player_loaded"))
            assertTrue(Protocol776.lookup(ProtocolState.PLAY, Direction.C2S, name).kept(), name);
    }

    @Test
    void testAuditNoRowsAreDropped() {
        for (String name : List.of("award_stats", "block_destruction", "boss_event", "clear_titles", "commands",
            "container_set_data", "custom_chat_completions", "delete_chat", "hurt_animation", "level_event",
            "level_particles", "map_item_data", "merchant_offers", "place_ghost_recipe", "player_chat",
            "player_combat_end", "player_combat_enter", "projectile_power", "reset_score", "rotate_head",
            "server_data", "set_default_spawn_position", "set_display_objective", "set_experience",
            "set_objective", "set_score", "sound", "sound_entity", "stop_sound", "system_chat", "tab_list",
            "take_item_entity", "update_recipes", "waypoint"))
            assertFalse(Protocol776.lookup(ProtocolState.PLAY, Direction.S2C, name).kept(), name);

        for (String name : List.of("chat", "chat_command", "edit_book", "rename_item", "set_beacon", "sign_update",
            "swing", "seen_advancements", "select_trade", "client_information"))
            assertFalse(Protocol776.lookup(ProtocolState.PLAY, Direction.C2S, name).kept(), name);
    }

    @Test
    void testPingSetIsTheAuditYesRowsMinusEntityAndScreenGroups() {
        var expected = new LinkedHashSet<String>(AUDIT_YES);
        expected.removeAll(ENTITY_GROUP);
        expected.removeAll(SCREEN_GROUP);
        // The one addition on top of the derivation rule: a piston moving-block entity is a live
        // collision shape, and block_event — the same piston subsystem — is fenced.
        expected.add("block_entity_data");

        var actual = new LinkedHashSet<String>();
        for (Protocol776.Entry entry : Protocol776.table(ProtocolState.PLAY, Direction.S2C))
            if (entry.pingSet()) actual.add(entry.name());

        assertEquals(new TreeSet<>(expected), new TreeSet<>(actual));
        assertEquals(34, actual.size());
    }

    /// The per-entity exclusion leaks for the local player, so the own-player-relevant members of
    /// the entity group are fenced conditionally: the table cannot decide on the id alone, the
    /// engine asks the decoded packet.
    @Test
    void testConditionalFencesCoverTheOwnPlayerLeaksOfTheEntityGroup() {
        var expected = Set.of("animate", "entity_event", "remove_mob_effect", "set_entity_data",
            "set_entity_motion", "update_attributes", "update_mob_effect");

        var actual = new LinkedHashSet<String>();
        for (Protocol776.Entry entry : Protocol776.table(ProtocolState.PLAY, Direction.S2C))
            if (entry.pingWhen() != null) actual.add(entry.name());

        assertEquals(new TreeSet<>(expected), new TreeSet<>(actual));
        for (String name : expected) {
            var entry = Protocol776.lookup(ProtocolState.PLAY, Direction.S2C, name);
            assertEquals(Protocol776.KeepKind.KEEP_DECODE, entry.kind(), name);
            assertFalse(entry.pingSet(), name + " cannot be both unconditional and conditional");
        }

        int local = 7;
        assertFence(true, "set_entity_motion", new S2CSetEntityMotion.V776(local, LpVec3.ZERO), local);
        assertFence(false, "set_entity_motion", new S2CSetEntityMotion.V776(9, LpVec3.ZERO), local);
        assertFence(true, "animate", new S2CAnimate.V776(9, S2CAnimate.WAKE_UP), local);
        assertFence(false, "animate", new S2CAnimate.V776(local, 0), local);
        assertFence(true, "entity_event", new S2CEntityEvent.V776(local, S2CEntityEvent.USE_ITEM_COMPLETE), local);
        assertFence(true, "entity_event", new S2CEntityEvent.V776(local, S2CEntityEvent.SWAP_HANDS), local);
        assertFence(false, "entity_event", new S2CEntityEvent.V776(local, (byte) 3), local);
        assertFence(false, "entity_event", new S2CEntityEvent.V776(9, S2CEntityEvent.SWAP_HANDS), local);
        assertFence(true, "set_entity_data", new S2CSetEntityData.V776(local, new byte[0]), local);
        assertFence(false, "update_attributes", new S2CUpdateAttributes.V776(9, new byte[0]), local);
    }

    @Test
    void testPingSetMembersAreAlwaysKept() {
        for (ProtocolState state : ProtocolState.values())
            for (Direction direction : Direction.values())
                for (Protocol776.Entry entry : Protocol776.table(state, direction))
                    if (entry.pingSet()) assertTrue(entry.kept(), entry.name());
    }

    @Test
    void testConfigurationPhaseHasNoPings() {
        for (Protocol776.Entry entry : Protocol776.table(ProtocolState.CONFIGURATION, Direction.S2C))
            assertFalse(entry.pingSet(), entry.name());
    }

    @Test
    void testUnknownIdsAndNamesResolveToDrop() {
        assertSame(Protocol776.UNKNOWN, Protocol776.lookup(ProtocolState.PLAY, Direction.S2C, 141));
        assertSame(Protocol776.UNKNOWN, Protocol776.lookup(ProtocolState.PLAY, Direction.S2C, -1));
        assertSame(Protocol776.UNKNOWN, Protocol776.lookup(ProtocolState.PLAY, Direction.S2C, "nonexistent"));
        assertEquals(-1, Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, "nonexistent"));
        assertFalse(Protocol776.UNKNOWN.kept());
        assertNull(Protocol776.UNKNOWN.decoder());
    }

    @Test
    void testLookupByIdAndNameAgree() {
        for (var state : ProtocolState.values()) {
            for (var direction : Direction.values()) {
                var entries = Protocol776.table(state, direction);
                for (int id = 0; id < entries.size(); id++) {
                    var entry = entries.get(id);
                    assertSame(entry, Protocol776.lookup(state, direction, id));
                    assertEquals(id, Protocol776.packetId(state, direction, entry.name()));
                }
            }
        }
    }

    private static int playS2C(String name) {
        return Protocol776.packetId(ProtocolState.PLAY, Direction.S2C, name);
    }

    private static void assertFence(boolean expected, String name, Packet packet, int localPlayerId) {
        var when = Protocol776.lookup(ProtocolState.PLAY, Direction.S2C, name).pingWhen();
        assertNotNull(when, name);
        assertEquals(expected, when.fence(packet, localPlayerId), name);
    }
}
