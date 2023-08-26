package dev.hollowcube.replay.change;

import dev.hollowcube.replay.RecordedChange;
import dev.hollowcube.replay.ReplayMetadata;
import dev.hollowcube.replay.util.Binary;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.minestom.server.network.NetworkBuffer.STRING;
import static net.minestom.server.network.NetworkBuffer.VAR_INT;

@SuppressWarnings("UnstableApiUsage")
public record RecordedPlayerSpawn(
        int entityId,
        @NotNull String username,
        @Nullable String skinTexture,
        @Nullable String skinSignature,
        @NotNull Pos pos
) implements RecordedChange {

    public RecordedPlayerSpawn(@NotNull ReplayMetadata metadata, @NotNull NetworkBuffer buffer) {
        this(buffer.read(VAR_INT), buffer.read(STRING), buffer.readOptional(STRING), buffer.readOptional(STRING),
                Binary.readRelativePos(buffer).add(metadata.origin()));
    }

    @Override
    public void write(@NotNull ReplayMetadata metadata, @NotNull NetworkBuffer buffer) {
        buffer.write(VAR_INT, entityId);
        buffer.write(STRING, username);
        buffer.writeOptional(STRING, skinTexture);
        buffer.writeOptional(STRING, skinSignature);
        Binary.writeRelativePos(buffer, pos.sub(metadata.origin()));
    }

    @Override
    public int id() {
        return ReplayChangeId.PLAYER_SPAWN;
    }

}
