package net.hollowcube.mapmaker.mod.packet.client;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("UnstableApiUsage")
public record HCClientModifyAnimationPacket(
        int sequence,
        @Nullable Integer currentTick,
        @Nullable Float playbackSpeed,
        @Nullable Boolean playing
) implements HCClientPlayPacket {

    public HCClientModifyAnimationPacket(@NotNull NetworkBuffer buffer, int apiVersion) {
        this(
                buffer.read(NetworkBuffer.VAR_INT),
                buffer.readOptional(NetworkBuffer.VAR_INT),
                buffer.readOptional(NetworkBuffer.FLOAT),
                buffer.readOptional(NetworkBuffer.BOOLEAN)
        );
    }

}
