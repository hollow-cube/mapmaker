package net.hollowcube.terraform.compat.axiom.packet.server;

import net.hollowcube.terraform.compat.axiom.Axiom;
import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * AxiomServerPacket is a {@link net.minestom.server.network.packet.server.play.PluginMessagePacket} with strict types.
 * <p>
 * It allows the use of {@link net.minestom.server.entity.Player#sendPacket(SendablePacket)} which is convenient.
 */
@SuppressWarnings("UnstableApiUsage")
public interface AxiomServerPacket {

    @NotNull String packetChannel();

    void write(@NotNull NetworkBuffer buffer, int apiVersion);

    /**
     * Converts the packet into a sendable {@link PluginMessagePacket} for the given player,
     * taking into account the player's axiom API version.
     *
     * @param player The player who will receive the packet
     * @return The sendable server packet
     */
    default @NotNull PluginMessagePacket toPacket(@NotNull Player player) {
        int apiVersion = Objects.requireNonNull(
                player.getTag(Axiom.CLIENT_INFO_TAG),
                "axiom info not present for player"
        ).apiVersion();
        return new PluginMessagePacket(packetChannel(), NetworkBuffer.makeArray(buffer -> write(buffer, apiVersion)));
    }

}
