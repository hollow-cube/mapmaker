package net.hollowcube.anticheat.protocol;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/// The packet id tables for protocol version 776 (26.2), in the registration order of
/// `GameProtocols` and `ConfigurationProtocols` — `ProtocolInfoBuilder` hands out ids as the index
/// into the list it builds, so position here *is* the packet id. Play S2C id 0 is the bundle
/// delimiter, which `withBundlePacket` registers first.
///
/// What is kept and what is decoded comes from `anticheat-packet-audit.md`; the ping set is that
/// document's YES rows minus the per-entity and screen-opening groups, per plan section 2.3, plus
/// `block_entity_data` (a piston moving-block entity is a live collision shape, and `block_event` —
/// the same subsystem — is fenced). The per-entity exclusion leaks for the local player, so the
/// own-player-relevant members of that group carry a [PingWhen] instead: fenced only when the
/// decoded packet lands on the local player (or, for `animate`, when the action writes a block).
public final class Protocol776 {

    public static final int PROTOCOL_VERSION = 776;

    @FunctionalInterface
    public interface Decoder {
        Packet decode(ByteReader reader);
    }

    /// A fence the table cannot decide on the id alone: asked of the decoded packet, once the
    /// engine has it, and a yes means a ping is due exactly as if the entry were in the ping set.
    @FunctionalInterface
    public interface PingWhen {
        boolean fence(Packet packet, int localPlayerId);
    }

    /// What the tap does with a frame.
    ///
    /// [#PING_SET] is [#KEEP] plus "a ping is injected after this frame". Decoded packets can be
    /// in the ping set too, so [Entry#pingSet()] — not the kind — is the query the tap makes;
    /// the kind only ever says what to do with the payload.
    public enum KeepKind {
        /// Not recorded, passed through untouched.
        DROP,
        /// Recorded as raw bytes.
        KEEP,
        /// Recorded as raw bytes and decoded into a [Packet] for the world model and trim.
        KEEP_DECODE,
        /// Recorded as raw bytes, and a state change the reader has to be able to time.
        PING_SET
    }

    public record Entry(String name, KeepKind kind, boolean pingSet, @Nullable Decoder decoder,
                        @Nullable PingWhen pingWhen) {

        public boolean kept() {
            return kind != KeepKind.DROP;
        }
    }

    /// Fence when the packet lands on the local player: what is per-entity noise for everyone else
    /// is knockback, a speed change or a hitbox change when the entity is the player themself.
    private static final PingWhen SELF =
        (packet, localPlayerId) -> packet instanceof EntityKeyed keyed && keyed.entityId() == localPlayerId;

    /// `stopSleeping` writes the bed block, teleports the entity and changes its pose — the only
    /// animate action that mutates the world, for any entity.
    private static final PingWhen WAKE_UP =
        (packet, _) -> packet instanceof S2CAnimate animate && animate.action() == S2CAnimate.WAKE_UP;

    /// Events 9 and 55 on the local player carry state no other packet does: the end of item use
    /// (the sprint gate) and the hand swap. Everything else in the packet is cosmetic.
    private static final PingWhen SELF_EVENT = (packet, localPlayerId) ->
        packet instanceof S2CEntityEvent event && event.entityId() == localPlayerId
            && (event.event() == S2CEntityEvent.USE_ITEM_COMPLETE || event.event() == S2CEntityEvent.SWAP_HANDS);

    /// The answer for an id the version does not define. Unknown ids are passed through, never
    /// recorded: a frame we cannot name is a frame we cannot replay.
    public static final Entry UNKNOWN = new Entry("unknown", KeepKind.DROP, false, null, null);

    private static final Table HANDSHAKE_C2S = new Builder()
        .drop("intention")
        .build();
    private static final Table LOGIN_C2S = new Builder()
        .drop("hello")
        .drop("key")
        .drop("custom_query_answer")
        .keep("login_acknowledged")
        .drop("cookie_response")
        .build();
    private static final Table LOGIN_S2C = new Builder()
        .keep("login_disconnect")
        .drop("hello")
        .keep("login_finished")
        .drop("login_compression")
        .drop("custom_query")
        .drop("cookie_request")
        .build();
    private static final Table CONFIGURATION_C2S = new Builder()
        .drop("client_information")
        .keep("cookie_response")
        .decode("custom_payload", C2SCustomPayload.V776::decode)
        .decode("finish_configuration", C2SFinishConfiguration.V776::decode)
        .keep("keep_alive")
        .decode("pong", C2SPong.V776::decode)
        .drop("resource_pack")
        .keep("select_known_packs")
        .drop("custom_click_action")
        .keep("accept_code_of_conduct")
        .build();
    private static final Table CONFIGURATION_S2C = new Builder()
        .keep("cookie_request")
        .decode("custom_payload", S2CCustomPayload.V776::decode)
        .keep("disconnect")
        .decode("finish_configuration", S2CFinishConfiguration.V776::decode)
        .keep("keep_alive")
        .decode("ping", S2CPing.V776::decode)
        .drop("reset_chat")
        .decode("registry_data", S2CRegistryData.V776::decode)
        .drop("resource_pack_pop")
        .keep("resource_pack_push")
        .drop("store_cookie")
        .keep("transfer")
        .keep("update_enabled_features")
        .decode("update_tags", S2CUpdateTags.V776::decode)
        .keep("select_known_packs")
        .drop("custom_report_details")
        .drop("server_links")
        .keep("clear_dialog")
        .keep("show_dialog")
        .keep("code_of_conduct")
        .build();
    private static final Table PLAY_C2S = new Builder()
        .keep("accept_teleportation")
        .keep("attack")
        .drop("block_entity_tag_query")
        .keep("bundle_item_selected")
        .drop("change_difficulty")
        .keep("change_game_mode")
        .drop("chat_ack")
        .drop("chat_command")
        .drop("chat_command_signed")
        .drop("chat")
        .drop("chat_session_update")
        .keep("chunk_batch_received")
        .keep("client_command")
        .keep("client_tick_end")
        .drop("client_information")
        .drop("command_suggestion")
        .decode("configuration_acknowledged", C2SConfigurationAcknowledged.V776::decode)
        .drop("container_button_click")
        .keep("container_click")
        .keep("container_close")
        .drop("container_slot_state_changed")
        .keep("cookie_response")
        .decode("custom_payload", C2SCustomPayload.V776::decode)
        .drop("debug_subscription_request")
        .drop("edit_book")
        .drop("entity_tag_query")
        .keep("interact")
        .drop("jigsaw_generate")
        .keep("keep_alive")
        .drop("lock_difficulty")
        .decode("move_player_pos", C2SMovePlayerPos.V776::decode)
        .decode("move_player_pos_rot", C2SMovePlayerPosRot.V776::decode)
        .decode("move_player_rot", C2SMovePlayerRot.V776::decode)
        .decode("move_player_status_only", C2SMovePlayerStatusOnly.V776::decode)
        .keep("move_vehicle")
        .keep("paddle_boat")
        .keep("pick_item_from_block")
        .keep("pick_item_from_entity")
        .drop("ping_request")
        .drop("place_recipe")
        .keep("player_abilities")
        .keep("player_action")
        .keep("player_command")
        .keep("player_input")
        .keep("player_loaded")
        .decode("pong", C2SPong.V776::decode)
        .drop("recipe_book_change_settings")
        .drop("recipe_book_seen_recipe")
        .drop("rename_item")
        .drop("resource_pack")
        .drop("seen_advancements")
        .drop("select_trade")
        .drop("set_beacon")
        .keep("set_carried_item")
        .drop("set_command_block")
        .drop("set_command_minecart")
        .keep("set_creative_mode_slot")
        .drop("set_game_rule")
        .drop("set_jigsaw_block")
        .drop("set_structure_block")
        .drop("set_test_block")
        .drop("sign_update")
        .keep("spectator_action")
        .drop("swing")
        .keep("teleport_to_entity")
        .drop("test_instance_block_action")
        .keep("use_item_on")
        .keep("use_item")
        .drop("custom_click_action")
        .build();
    private static final Table PLAY_S2C = new Builder()
        .decode("bundle_delimiter", S2CBundleDelimiter.V776::decode)
        .decode("add_entity", S2CAddEntity.V776::decode)
        .decodePingWhen("animate", S2CAnimate.V776::decode, WAKE_UP)
        .drop("award_stats")
        .ping("block_changed_ack")
        .drop("block_destruction")
        // A piston moving-block entity is a live collision shape; block_event, the same piston
        // subsystem, is fenced, so this is too.
        .ping("block_entity_data")
        .ping("block_event")
        .decodePing("block_update", S2CBlockUpdate.V776::decode)
        .drop("boss_event")
        .drop("change_difficulty")
        .keep("chunk_batch_finished")
        .keep("chunk_batch_start")
        .keep("chunks_biomes")
        .drop("clear_titles")
        .drop("command_suggestions")
        .drop("commands")
        .keep("container_close")
        .decodePing("container_set_content", S2CContainerSetContent.V776::decode)
        .drop("container_set_data")
        .decodePing("container_set_slot", S2CContainerSetSlot.V776::decode)
        .keep("cookie_request")
        .ping("cooldown")
        .drop("custom_chat_completions")
        .decode("custom_payload", S2CCustomPayload.V776::decode)
        .keep("damage_event")
        .drop("debug/block_value")
        .drop("debug/chunk_value")
        .drop("debug/entity_value")
        .drop("debug/event")
        .drop("debug_sample")
        .drop("delete_chat")
        .keep("disconnect")
        .drop("disguised_chat")
        .decodePingWhen("entity_event", S2CEntityEvent.V776::decode, SELF_EVENT)
        // Unfenced even when it moves the player through their vehicle: a non-interpolated update
        // to the vehicle forces a C2S move_player echo, and that echo is the de-facto fence. The
        // same goes for teleport_entity and move_vehicle below.
        .decode("entity_position_sync", S2CEntityPositionSync.V776::decode)
        .ping("explode")
        .decodePing("forget_level_chunk", S2CForgetLevelChunk.V776::decode)
        .ping("game_event")
        .drop("game_rule_values")
        .drop("game_test_highlight_pos")
        .keep("mount_screen_open")
        .drop("hurt_animation")
        .ping("initialize_border")
        .keep("keep_alive")
        .decodePing("level_chunk_with_light", S2CLevelChunkWithLight.V776::decode)
        .drop("level_event")
        .drop("level_particles")
        .keep("light_update")
        .decodePing("login", S2CLogin.V776::decode)
        .drop("low_disk_space_warning")
        .drop("map_item_data")
        .drop("merchant_offers")
        .decode("move_entity_pos", S2CMoveEntityPos.V776::decode)
        .decode("move_entity_pos_rot", S2CMoveEntityPosRot.V776::decode)
        .keep("move_minecart_along_track")
        .decode("move_entity_rot", S2CMoveEntityRot.V776::decode)
        .keep("move_vehicle")
        .keep("open_book")
        .keep("open_screen")
        .keep("open_sign_editor")
        .decode("ping", S2CPing.V776::decode)
        .drop("pong_response")
        .drop("place_ghost_recipe")
        .ping("player_abilities")
        .drop("player_chat")
        .drop("player_combat_end")
        .drop("player_combat_enter")
        .keep("player_combat_kill")
        .keep("player_info_remove")
        .ping("player_info_update")
        .ping("player_look_at")
        .decodePing("player_position", S2CPlayerPosition.V776::decode)
        .decodePing("player_rotation", S2CPlayerRotation.V776::decode)
        .drop("recipe_book_add")
        .drop("recipe_book_remove")
        .drop("recipe_book_settings")
        .decode("remove_entities", S2CRemoveEntities.V776::decode)
        // Fenced for self together with update_attributes: removing an effect leaves its
        // attribute modifiers in place until the following update_attributes, so the relative
        // timing of the two packets is itself load-bearing.
        .decodePingWhen("remove_mob_effect", S2CRemoveMobEffect.V776::decode, SELF)
        .drop("reset_score")
        .drop("resource_pack_pop")
        .keep("resource_pack_push")
        .decodePing("respawn", S2CRespawn.V776::decode)
        .drop("rotate_head")
        .decodePing("section_blocks_update", S2CSectionBlocksUpdate.V776::decode)
        .drop("select_advancements_tab")
        .drop("server_data")
        .drop("set_action_bar_text")
        .ping("set_border_center")
        .ping("set_border_lerp_size")
        .ping("set_border_size")
        .drop("set_border_warning_delay")
        .drop("set_border_warning_distance")
        .ping("set_camera")
        .decodePing("set_chunk_cache_center", S2CSetChunkCacheCenter.V776::decode)
        .decodePing("set_chunk_cache_radius", S2CSetChunkCacheRadius.V776::decode)
        .keep("set_cursor_item")
        .drop("set_default_spawn_position")
        .drop("set_display_objective")
        .decodePingWhen("set_entity_data", S2CSetEntityData.V776::decode, SELF)
        .decode("set_entity_link", S2CSetEntityLink.V776::decode)
        .decodePingWhen("set_entity_motion", S2CSetEntityMotion.V776::decode, SELF)
        .decode("set_equipment", S2CSetEquipment.V776::decode)
        .drop("set_experience")
        .ping("set_health")
        .ping("set_held_slot")
        .drop("set_objective")
        .decode("set_passengers", S2CSetPassengers.V776::decode)
        .decodePing("set_player_inventory", S2CSetPlayerInventory.V776::decode)
        .ping("set_player_team")
        .drop("set_score")
        .keep("set_simulation_distance")
        .drop("set_subtitle_text")
        .ping("set_time")
        .drop("set_title_text")
        .drop("set_titles_animation")
        .drop("sound_entity")
        .drop("sound")
        .decode("start_configuration", S2CStartConfiguration.V776::decode)
        .drop("stop_sound")
        .drop("store_cookie")
        .drop("system_chat")
        .drop("tab_list")
        .drop("tag_query")
        .drop("take_item_entity")
        .decode("teleport_entity", S2CTeleportEntity.V776::decode)
        .drop("test_instance_block_status")
        .ping("ticking_state")
        .ping("ticking_step")
        // Terminal frames are never fenced: a ping written right before the kick may never be
        // answered, and a dangling id looks identical to a lagging client. The trace's
        // tailUnfenced header flag is the honest signal that the final window has no upper bound.
        .keep("transfer")
        .drop("update_advancements")
        .decodePingWhen("update_attributes", S2CUpdateAttributes.V776::decode, SELF)
        .decodePingWhen("update_mob_effect", S2CUpdateMobEffect.V776::decode, SELF)
        .drop("update_recipes")
        .decodePing("update_tags", S2CUpdateTags.V776::decode)
        .drop("projectile_power")
        .drop("custom_report_details")
        .drop("server_links")
        .drop("waypoint")
        .keep("clear_dialog")
        .keep("show_dialog")
        .build();

    public static List<Entry> table(ProtocolState state, Direction direction) {
        return select(state, direction).entries;
    }

    public static Entry lookup(ProtocolState state, Direction direction, int packetId) {
        var entries = select(state, direction).entries;
        return packetId >= 0 && packetId < entries.size() ? entries.get(packetId) : UNKNOWN;
    }

    /// Lookup by the vanilla packet type name (`GamePacketTypes` and friends, without the
    /// `minecraft:` namespace), so tests can name packets the way the audit does.
    public static Entry lookup(ProtocolState state, Direction direction, String name) {
        var id = select(state, direction).ids.get(name);
        return id == null ? UNKNOWN : select(state, direction).entries.get(id);
    }

    /// The packet id of a named packet, or -1 when the version does not have it.
    public static int packetId(ProtocolState state, Direction direction, String name) {
        var id = select(state, direction).ids.get(name);
        return id == null ? -1 : id;
    }

    private static Table select(ProtocolState state, Direction direction) {
        return switch (state) {
            case HANDSHAKE -> switch (direction) {
                case C2S -> HANDSHAKE_C2S;
                case S2C -> Table.EMPTY;
            };
            case LOGIN -> switch (direction) {
                case C2S -> LOGIN_C2S;
                case S2C -> LOGIN_S2C;
            };
            case CONFIGURATION -> switch (direction) {
                case C2S -> CONFIGURATION_C2S;
                case S2C -> CONFIGURATION_S2C;
            };
            case PLAY -> switch (direction) {
                case C2S -> PLAY_C2S;
                case S2C -> PLAY_S2C;
            };
        };
    }

    private record Table(List<Entry> entries, Map<String, Integer> ids) {
        static final Table EMPTY = new Table(List.of(), Map.of());
    }

    private static final class Builder {
        private final List<Entry> entries = new ArrayList<>();

        Builder drop(String name) {
            return add(new Entry(name, KeepKind.DROP, false, null, null));
        }

        Builder keep(String name) {
            return add(new Entry(name, KeepKind.KEEP, false, null, null));
        }

        Builder ping(String name) {
            return add(new Entry(name, KeepKind.PING_SET, true, null, null));
        }

        Builder decode(String name, Decoder decoder) {
            return add(new Entry(name, KeepKind.KEEP_DECODE, false, decoder, null));
        }

        Builder decodePing(String name, Decoder decoder) {
            return add(new Entry(name, KeepKind.KEEP_DECODE, true, decoder, null));
        }

        Builder decodePingWhen(String name, Decoder decoder, PingWhen when) {
            return add(new Entry(name, KeepKind.KEEP_DECODE, false, decoder, when));
        }

        private Builder add(Entry entry) {
            entries.add(entry);
            return this;
        }

        Table build() {
            var ids = new HashMap<String, Integer>(entries.size());
            for (int id = 0; id < entries.size(); id++) ids.put(entries.get(id).name(), id);
            return new Table(List.copyOf(entries), Map.copyOf(ids));
        }
    }

    private Protocol776() {}
}
