package dev.hollowcube.replay;

import net.minestom.server.network.NetworkBuffer;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings("UnstableApiUsage")
public interface RecordedChange {

    void write(@NotNull ReplayMetadata metadata, @NotNull NetworkBuffer buffer);

    int id();

    interface Reader {
        @NotNull RecordedChange read(@NotNull ReplayMetadata metadata, @NotNull NetworkBuffer buffer);
    }

}
