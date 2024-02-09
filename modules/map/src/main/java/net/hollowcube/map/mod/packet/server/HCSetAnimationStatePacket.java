package net.hollowcube.map.mod.packet.server;

import net.hollowcube.mapmaker.mod.packet.server.HCServerPlayPacket;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public record HCSetAnimationStatePacket(
        int currentTick,
        float playbackSpeed
) implements HCServerPlayPacket {
    @Override
    public @NotNull String packetChannel() {
        return "hollowcube:set_animation_state";
    }

    @Override
    public void write0(@NotNull NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.VAR_INT, currentTick);
        buffer.write(NetworkBuffer.FLOAT, playbackSpeed);
    }
}
