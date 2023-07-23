package dev.hollowcube.replay.change;

import dev.hollowcube.replay.RecordedChange;
import dev.hollowcube.replay.ReplayMetadata;
import dev.hollowcube.replay.util.Binary;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

@SuppressWarnings("UnstableApiUsage")
public record RecordedPlayerMove(
        int entityId,
        @NotNull Pos pos
) implements RecordedChange {

    public RecordedPlayerMove(@NotNull ReplayMetadata metadata, @NotNull NetworkBuffer buffer) {
        this(buffer.read(VAR_INT), Binary.readRelativePos(buffer).add(metadata.origin()));
    }

    @Override
    public void write(@NotNull ReplayMetadata metadata, @NotNull NetworkBuffer buffer) {
        buffer.write(VAR_INT, entityId);
        Binary.writeRelativePos(buffer, pos.sub(metadata.origin()));
    }

    @Override
    public int id() {
        return ReplayChangeId.PLAYER_POSITION;
    }
}
