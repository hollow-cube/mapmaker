package net.hollowcube.mapmaker.map.util.metrics;

import net.hollowcube.mapmaker.metrics.Metric;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public class MapInstanceCreatedEvent implements Metric {
    private final Instant timestamp = Instant.now();
    private final String mapId;
    private final String worldId;

    public MapInstanceCreatedEvent(@NotNull String mapId, @NotNull String worldId) {
        this.mapId = mapId;
        this.worldId = worldId;
    }

    @Override
    public @NotNull Instant timestamp() {
        return timestamp;
    }

}
