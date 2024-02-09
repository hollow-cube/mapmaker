package net.hollowcube.mapmaker.mod.packet.server;

import net.minestom.server.network.ConnectionState;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import net.minestom.server.utils.PacketUtils;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public sealed interface HCServerPacket extends ServerPacket permits HCServerConfigPacket, HCServerPlayPacket {

    @NotNull String packetChannel();

    @Override
    default void write(@NotNull NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.STRING, packetChannel());
        buffer.write(NetworkBuffer.RAW_BYTES, NetworkBuffer.makeArray(this::write0));
    }

    void write0(@NotNull NetworkBuffer buffer);

    default int getId(@NotNull ConnectionState state) {
        return switch (state) {
            case CONFIGURATION -> ServerPacketIdentifier.CONFIGURATION_PLUGIN_MESSAGE;
            case PLAY -> ServerPacketIdentifier.PLUGIN_MESSAGE;
            default ->
                    PacketUtils.invalidPacketState(this.getClass(), state, ConnectionState.CONFIGURATION, ConnectionState.PLAY);
        };
    }

}
