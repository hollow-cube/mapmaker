package net.hollowcube.mapmaker.metrics;

import org.jetbrains.annotations.NotNull;

import java.time.Instant;

public interface Metric {

    /**
     * Exists just to force implementations to add a timestamp field, which should be present on every metric.
     */
    @NotNull Instant timestamp();
}
