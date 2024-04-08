package net.hollowcube.terraform.compat.axiom;

import net.hollowcube.terraform.compat.axiom.packet.client.*;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomEnablePacket;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomServerPacket;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public class Axiom {

    public static final int MIN_API_VERSION = 7;
    public static final int MAX_API_VERSION = 7;


    // Config properties

    /**
     * The maximum number of sections which can be updated in a single (correlated) change.
     */
    public static final int MAX_SECTIONS_PER_UPDATE = Integer.getInteger("terraform.axiom.max_sections_per_update", 1024 * 1024);

    /**
     * The block state sent in block buffers to indicate no change to the world.
     */
    public static final int EMPTY_BLOCK_STATE = Block.STRUCTURE_VOID.stateId();


    public record ClientInfo(int apiVersion, @NotNull NBTCompound extraData) {
    }

    static final Tag<Boolean> ENABLED_TAG = Tag.Boolean("terraform:axiom/enabled");
    public static final Tag<ClientInfo> CLIENT_INFO_TAG = Tag.Structure("terraform:axiom/client_info", ClientInfo.class);

    public static void sendPacket(@NotNull Player player, @NotNull AxiomServerPacket packet) {
        if (!Axiom.isPresent(player) || !Axiom.isEnabled(player)) return;
        player.sendPacket(packet.toPacket(player));
    }

    public static void sendPacket(@NotNull Collection<Player> players, @NotNull AxiomServerPacket packet) {
        for (var player : players) {
            if (!Axiom.isPresent(player) || !Axiom.isEnabled(player))
                continue;
            player.sendPacket(packet.toPacket(player));
        }
    }

    public static void sendPacket(@NotNull Instance instance, @NotNull AxiomServerPacket packet) {
        sendPacket(instance.getPlayers(), packet);
    }

    /**
     * Returns true if the player has axiom installed on their client. This does not say whether it is currently enabled.
     */
    public static boolean isPresent(@NotNull Player player) {
        return player.hasTag(CLIENT_INFO_TAG);
    }

    public static boolean isEnabled(@NotNull Player player) {
        return player.hasTag(ENABLED_TAG);
    }

    /**
     * Enables Axiom functionality for a given player.
     *
     * <p>This will work even if the Axiom enable packet has not been received yet, it will be enabled as soon as we
     * recieve that message.</p>
     *
     * @param player
     */
    public static void enable(@NotNull Player player) {
        if (!isPresent(player)) {
            // Not present yet, just set the enabled tag and wait for the hello packet
            player.setTag(ENABLED_TAG, true);
            return;
        }

        // Axiom is present, perform the enable sequence.
        var enablePacket = new AxiomEnablePacket(
                true,
                0x100000, // 1mb, todo: constant/configurable
                false, false,
                5, // todo: constant/configurable
                16, // todo: constant/configurable
                true, // todo: constant/configurable
                List.of() //todo: constant/configurable
        );
        player.sendPacket(enablePacket.toPacket(player));
        player.setTag(ENABLED_TAG, true);

        //todo init hotbars
        //todo init views
    }

    public static void disable(@NotNull Player player) {
        if (!isEnabled(player)) return;
        player.removeTag(ENABLED_TAG);

        var packet = new AxiomEnablePacket(false);
        player.sendPacket(packet.toPacket(player));
    }

    private static final Map<String, ReadableAxiomPacket<?>> CLIENT_PACKETS = Map.ofEntries(
            Map.entry("axiom:hello", AxiomClientHelloPacket::new),
            Map.entry("axiom:set_gamemode", AxiomClientSetGameModePacket::new),
            Map.entry("axiom:set_fly_speed", AxiomClientSetFlySpeedPacket::new),
            Map.entry("axiom:set_hotbar_slot", AxiomClientSetHotbarSlotPacket::new),
            Map.entry("axiom:switch_active_hotbar", AxiomClientSwitchActiveHotbarPacket::new),
            Map.entry("axiom:teleport", AxiomClientTeleportPacket::new),
            Map.entry("axiom:set_editor_views", AxiomClientSetEditorViewsPacket::new),
            Map.entry("axiom:request_chunk_data", AxiomClientChunkDataRequestPacket::new),
            Map.entry("axiom:set_block", AxiomClientSetBlockPacket::new),
            Map.entry("axiom:set_buffer", AxiomClientSetBufferPacket::new),
            Map.entry("axiom:set_world_property", AxiomClientSetWorldPropertyPacket::new),
            Map.entry("axiom:set_time", AxiomClientSetTimePacket::new),
            Map.entry("axiom:spawn_entity", AxiomClientSpawnEntitiesPacket::new),
            Map.entry("axiom:manipulate_entity", AxiomClientModifyEntitiesPacket::new),
            Map.entry("axiom:delete_entity", AxiomClientDeleteEntitiesPacket::new),
            Map.entry("axiom:marker_nbt_request", AxiomClientMarkerNbtRequestPacket::new)
    );

    /**
     * All the axiom channels which the server will read from.
     */
    static final List<String> INCOMING_CHANNELS = List.copyOf(CLIENT_PACKETS.keySet());

    /**
     * All the axiom channels which the server supports (will handle).
     */
    static final List<String> OUTGOING_CHANNELS = List.of(
            "axiom:enable",
            "axiom:ack_world_properties",
            "axiom:response_chunk_data",
            "axiom:set_editor_views",
            "axiom:set_world_property",
            "axiom:initialize_hotbars",
            "axiom:custom_blocks", // todo
            "axiom:editor_warning", // todo
            "axiom:register_world_properties",
            "axiom:marker_nbt_response",
            "axiom:marker_data"
    );

    private interface ReadableAxiomPacket<T extends AxiomClientPacket> {
        @NotNull T read(@NotNull NetworkBuffer buffer, int apiVersion);
    }

    public static @Nullable AxiomClientPacket readPacket(@NotNull PlayerPluginMessageEvent event) {
        var reader = CLIENT_PACKETS.get(event.getIdentifier());
        if (reader == null) return null;
        var buffer = new NetworkBuffer(ByteBuffer.wrap(event.getMessage()));

        // When we receive the hello packet there will be no client info, so default to
        // the highest supported API version. It is present in the hello packet anyway.
        var clientInfo = event.getPlayer().getTag(CLIENT_INFO_TAG);
        var apiVersion = clientInfo == null ? MAX_API_VERSION : clientInfo.apiVersion();
        return reader.read(buffer, apiVersion);
    }

    private Axiom() {
    }
}
