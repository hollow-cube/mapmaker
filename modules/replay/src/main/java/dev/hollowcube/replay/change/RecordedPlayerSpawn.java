package dev.hollowcube.replay.change;

import dev.hollowcube.replay.RecordedChange;
import dev.hollowcube.replay.ReplayMetadata;
import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

import static net.minestom.server.network.NetworkBuffer.VAR_INT;

@SuppressWarnings("UnstableApiUsage")
public record RecordedPlayerSpawn(int entityId) implements RecordedChange {

    public RecordedPlayerSpawn(@NotNull ReplayMetadata metadata, @NotNull NetworkBuffer buffer) {
        this(buffer.read(VAR_INT));
    }

    @Override
    public void write(@NotNull ReplayMetadata metadata, @NotNull NetworkBuffer buffer) {
        buffer.write(VAR_INT, entityId);
    }

    @Override
    public int id() {
        return ReplayChangeId.PLAYER_SPAWN;
    }

}
