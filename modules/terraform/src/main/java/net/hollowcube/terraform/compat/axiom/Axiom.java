package net.hollowcube.terraform.compat.axiom;

import net.hollowcube.terraform.compat.axiom.packet.client.*;
import net.hollowcube.terraform.compat.axiom.packet.server.AxiomEnablePacket;
import net.minestom.server.entity.Player;
import net.minestom.server.event.player.PlayerPluginMessageEvent;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;

import java.nio.ByteBuffer;
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
    public static final int MAX_SECTIONS_PER_UPDATE = Integer.getInteger("terraform.axiom.max_sections_per_update", 1024);


    public record ClientInfo(int apiVersion, @NotNull NBTCompound extraData) {
    }

    static final Tag<Boolean> ENABLED_TAG = Tag.Boolean("terraform:axiom/enabled");
    public static final Tag<ClientInfo> CLIENT_INFO_TAG = Tag.Structure("terraform:axiom/client_info", ClientInfo.class);

    /**
     * Returns true if the player has axiom installed on their client. This does not say whether it is currently enabled.
     */
    public static boolean isPresent(@NotNull Player player) {
        return player.hasTag(CLIENT_INFO_TAG);
    }

    public static boolean isEnabled(@NotNull Player player) {
        return player.hasTag(ENABLED_TAG);
    }

    public static void enable(@NotNull Player player) {
        Check.stateCondition(!isPresent(player), "Axiom is not present on the client");

        var enablePacket = new AxiomEnablePacket(
                true,
                0x100000, // 1mb, todo: constant/configurable
                false, false,
                5, // todo: constant/configurable
                16, // todo: constant/configurable
                true // todo: constant/configurable
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

    /**
     * All the axiom channels which the server will read from.
     */
    static final List<String> INCOMING_CHANNELS = List.of(
            "axiom:hello", "axiom:set_gamemode",
            "axiom:set_fly_speed", "axiom:set_block",
            "axiom:set_hotbar_slot", "axiom:switch_active_hotbar",
            "axiom:teleport", "axiom:set_editor_views",
            "axiom:request_block_entity"
    );

    /**
     * All the axiom channels which the server supports (will handle).
     */
    static final List<String> OUTGOING_CHANNELS = List.of(
            "axiom:enable"
//        "axiom:initialize_hotbars",
//        "axiom:set_editor_views",
//        "axiom:block_entities"
    );

    private interface ReadableAxiomPacket<T extends AxiomClientPacket> {
        @NotNull T read(@NotNull NetworkBuffer buffer, int apiVersion);
    }

    private static final Map<String, ReadableAxiomPacket<?>> CLIENT_PACKETS = Map.of(
            "axiom:hello", AxiomClientHelloPacket::new,
            "axiom:set_gamemode", AxiomClientSetGameModePacket::new,
            "axiom:set_fly_speed", AxiomClientSetFlySpeedPacket::new,
            "axiom:set_block", AxiomClientSetBlockPacket::new,
            "axiom:teleport", AxiomClientTeleportPacket::new,
            "axiom:request_block_entity", AxiomClientRequestBlockEntityPacket::new,
            "axiom:set_buffer", AxiomClientSetBufferPacket::new,
            "axiom:set_world_property", AxiomClientSetWorldPropertyPacket::new
    );

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

    /**
     * The block state sent in block buffers to indicate no change to the world.
     */
    static final int EMPTY_BLOCK_STATE = Block.STRUCTURE_VOID.stateId();

    private Axiom() {
    }
}
