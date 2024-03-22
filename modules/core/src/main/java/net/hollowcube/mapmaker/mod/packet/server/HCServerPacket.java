package net.hollowcube.mapmaker.mod.packet.server;

import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.ServerPacketIdentifier;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public sealed interface HCServerPacket extends ServerPacket.Play, ServerPacket.Configuration permits HCServerConfigPacket, HCServerPlayPacket {

    @NotNull String packetChannel();

    @Override
    default void write(@NotNull NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.STRING, packetChannel());
        buffer.write(NetworkBuffer.RAW_BYTES, NetworkBuffer.makeArray(this::write0));
    }

    void write0(@NotNull NetworkBuffer buffer);

    @Override
    default int playId() {
        return ServerPacketIdentifier.PLUGIN_MESSAGE;
    }

    @Override
    default int configurationId() {
        return ServerPacketIdentifier.CONFIGURATION_PLUGIN_MESSAGE;
    }
}
