package net.hollowcube.mapmaker.map.util.metrics;

import net.hollowcube.mapmaker.metrics.Metric;
import org.jetbrains.annotations.NotNull;

public record MapInstanceCreatedEvent(@NotNull String mapId, @NotNull String worldId) implements Metric {

}
