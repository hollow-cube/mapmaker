package net.hollowcube.terraform.compat.axiom;

import net.hollowcube.terraform.compat.axiom.packet.AxiomClientPacket;
import net.hollowcube.terraform.compat.axiom.packet.AxiomPacketRegistry;
import net.hollowcube.terraform.compat.axiom.packet.AxiomServerPacket;
import net.hollowcube.terraform.compat.axiom.packet.client.*;
import net.hollowcube.terraform.compat.axiom.packet.server.*;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

/*

REMAINING TODOS FROM 1.21.2
- Re-add world properties
- Support biome buffers
- Finish chunk data response
FROM SERVER
- Add custom blocks packet
- Add editor warning packet
- Add initialize hotbars packet
- Add response entity data
- Add restrictions
- Add serverside set editor views
- Blueprint manifest
- Blueprint response
FROM CLIENT
- Add request entity data
- Request blueprint
- Upload blueprint
 */

@SuppressWarnings("UnstableApiUsage")
public class Axiom {

    public static final int MIN_API_VERSION = 8;
    public static final int MAX_API_VERSION = 8;

    public static final AxiomPacketRegistry<AxiomClientPacket> CLIENT_PACKETS = new AxiomPacketRegistry<>() {{
        register("axiom:annotation_update", AxiomClientAnnotationUpdatePacket.class, AxiomClientAnnotationUpdatePacket.SERIALIZER);
        register("axiom:request_chunk_data", AxiomClientChunkDataRequestPacket.class, AxiomClientChunkDataRequestPacket.SERIALIZER);
        register("axiom:delete_entity", AxiomClientDeleteEntitiesPacket.class, AxiomClientDeleteEntitiesPacket.SERIALIZER);
        register("axiom:hello", AxiomClientHelloPacket.class, AxiomClientHelloPacket.SERIALIZER);
        register("axiom:marker_nbt_request", AxiomClientMarkerNbtRequestPacket.class, AxiomClientMarkerNbtRequestPacket.SERIALIZER);
        register("axiom:manipulate_entity", AxiomClientModifyEntitiesPacket.class, AxiomClientModifyEntitiesPacket.SERIALIZER);
        register("axiom:set_block", AxiomClientSetBlockPacket.class, AxiomClientSetBlockPacket.SERIALIZER);
        register("axiom:set_buffer", AxiomClientSetBufferPacket.class, AxiomClientSetBufferPacket.SERIALIZER);
        register("axiom:set_editor_views", AxiomClientSetEditorViewsPacket.class, AxiomClientSetEditorViewsPacket.SERIALIZER);
        register("axiom:set_fly_speed", AxiomClientSetFlySpeedPacket.class, AxiomClientSetFlySpeedPacket.SERIALIZER);
        register("axiom:set_gamemode", AxiomClientSetGameModePacket.class, AxiomClientSetGameModePacket.SERIALIZER);
        register("axiom:set_hotbar_slot", AxiomClientSetHotbarSlotPacket.class, AxiomClientSetHotbarSlotPacket.SERIALIZER);
        register("axiom:set_time", AxiomClientSetTimePacket.class, AxiomClientSetTimePacket.SERIALIZER);
//        register("axiom:set_world_property", AxiomClientSetWorldPropertyPacket.class, AxiomClientSetWorldPropertyPacket.SERIALIZER);
        register("axiom:spawn_entity", AxiomClientSpawnEntitiesPacket.class, AxiomClientSpawnEntitiesPacket.SERIALIZER);
        register("axiom:switch_active_hotbar", AxiomClientSwitchActiveHotbarPacket.class, AxiomClientSwitchActiveHotbarPacket.SERIALIZER);
        register("axiom:teleport", AxiomClientTeleportPacket.class, AxiomClientTeleportPacket.SERIALIZER);
    }};
    public static final AxiomPacketRegistry<AxiomServerPacket> SERVER_PACKETS = new AxiomPacketRegistry<>() {{
        register("axiom:ack_world_properties", AxiomAckWorldPropertyPacket.class, AxiomAckWorldPropertyPacket.SERIALIZER);
        register("axiom:annotation_update", AxiomAnnotationUpdatePacket.class, AxiomAnnotationUpdatePacket.SERIALIZER);
//        register("axiom:response_chunk_data", AxiomChunkDataResponsePacket.class, AxiomChunkDataResponsePacket.SERIALIZER);
        register("axiom:enable", AxiomEnablePacket.class, AxiomEnablePacket.SERIALIZER);
        register("axiom:marker_data", AxiomMarkerDataPacket.class, AxiomMarkerDataPacket.SERIALIZER);
        register("axiom:marker_nbt_response", AxiomMarkerNbtResponsePacket.class, AxiomMarkerNbtResponsePacket.SERIALIZER);
//        register("axiom:register_world_properties", AxiomRegisterWorldPropertiesPacket.class, AxiomRegisterWorldPropertiesPacket.SERIALIZER);
//        register("axiom:set_world_property", AxiomSetWorldPropertyPacket.class, AxiomSetWorldPropertyPacket.SERIALIZER);
    }};


    // Config properties

    /**
     * The maximum number of sections which can be updated in a single (correlated) change.
     */
    public static final int MAX_SECTIONS_PER_UPDATE = Integer.getInteger("terraform.axiom.max_sections_per_update", 1024 * 1024);

    /**
     * The block state sent in block buffers to indicate no change to the world.
     */
    public static final int EMPTY_BLOCK_STATE = Block.STRUCTURE_VOID.stateId();


    public record ClientInfo(int apiVersion) {
    }

    static final Tag<Boolean> ENABLED_TAG = Tag.Boolean("terraform:axiom/enabled");
    public static final Tag<ClientInfo> CLIENT_INFO_TAG = Tag.Structure("terraform:axiom/client_info", ClientInfo.class);

    public static @NotNull PluginMessagePacket writePacket(@NotNull AxiomServerPacket packet) {
        final AxiomPacketRegistry.PacketInfo<AxiomServerPacket> spec = SERVER_PACKETS.packetInfo(packet.getClass());
        final byte[] packetData = NetworkBuffer.makeArray(spec.serializer(), packet);
        return new PluginMessagePacket(spec.channel(), packetData);
    }

    public static void sendPacket(@NotNull Player player, @NotNull AxiomServerPacket packet) {
        if (!Axiom.isPresent(player) || !Axiom.isEnabled(player)) return;
        player.sendPacket(writePacket(packet));
    }

    public static void sendPacket(@NotNull Collection<Player> players, @NotNull AxiomServerPacket packet) {
        for (var player : players) {
            if (!Axiom.isPresent(player) || !Axiom.isEnabled(player))
                continue;
            sendPacket(player, packet);
        }
    }

    public static void sendPacket(@NotNull Instance instance, @NotNull AxiomServerPacket packet) {
        sendPacket(instance.getPlayers(), packet);
    }

    public static @Nullable AxiomClientPacket readPacket(@NotNull PlayerPluginMessageEvent event) {
        final NetworkBuffer.Type<AxiomClientPacket> packetType = CLIENT_PACKETS.packetInfo(event.getIdentifier());
        if (packetType == null) return null;
        return NetworkBuffer.wrap(event.getMessage(), 0, event.getMessage().length).read(packetType);
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
        var enablePacket = new AxiomEnablePacket(new AxiomEnablePacket.ServerConfig(
                0x100000, // 1mb, todo: constant/configurable
                false, false,
                5, // todo: constant/configurable
                16, // todo: constant/configurable
                true, // todo: constant/configurable
                List.of(), //todo: constant/configurable
                List.of(), //todo: constant/configurable
                1 //todo: constant/configurable
        ));
        sendPacket(player, enablePacket);
        player.setTag(ENABLED_TAG, true);

        //todo init hotbars
        //todo init views
    }

    public static void disable(@NotNull Player player) {
        if (!isEnabled(player)) return;
        player.removeTag(ENABLED_TAG);

        sendPacket(player, new AxiomEnablePacket(null));
    }

    private Axiom() {
    }
}
