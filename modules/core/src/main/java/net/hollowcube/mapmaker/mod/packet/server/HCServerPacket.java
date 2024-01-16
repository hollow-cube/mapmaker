package net.hollowcube.mapmaker.mod.packet.server;

import net.minestom.server.entity.Player;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public sealed interface HCServerPacket permits HCServerConfigPacket, HCServerPlayPacket {

    @NotNull String packetChannel();

    void write(@NotNull NetworkBuffer buffer, int apiVersion);

    /**
     * Converts the packet into a sendable {@link PluginMessagePacket} for the given player,
     * taking into account the player's hollowcube mod version.
     *
     * @param player The player who will receive the packet
     * @return The sendable server packet
     */
    default @NotNull PluginMessagePacket toPacket(@NotNull Player player) {
        int apiVersion = 1;

        // Ensure we are in the correct state for the packet.
        switch (player.getPlayerConnection().getConnectionState()) {
            case HANDSHAKE, STATUS, LOGIN -> Check.stateCondition(true,
                    "Cannot send packet before configuration state");
            case CONFIGURATION -> Check.stateCondition(
                    !(this instanceof HCServerConfigPacket),
                    "Cannot send server packet while in configuration state");
            case PLAY -> Check.stateCondition(
                    !(this instanceof HCServerPlayPacket),
                    "Cannot send server packet while in configuration state");
        }

        return new PluginMessagePacket(packetChannel(), NetworkBuffer.makeArray(buffer -> write(buffer, apiVersion)));
    }

}
