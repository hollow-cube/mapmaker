package net.hollowcube.map.mod.packet.server;

import net.hollowcube.mapmaker.mod.packet.server.HCServerPlayPacket;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public record HCAckAnimationChangePacket(int sequence) implements HCServerPlayPacket {
    @Override
    public @NotNull String packetChannel() {
        return "hollowcube:ack_animation_change";
    }

    @Override
    public void write0(@NotNull NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.VAR_INT, sequence);
    }
}
