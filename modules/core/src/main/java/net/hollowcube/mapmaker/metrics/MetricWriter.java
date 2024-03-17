package net.hollowcube.mapmaker.metrics;

import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

@NonBlocking
public interface MetricWriter {

    /**
     * Asynchronously writes a metric to the remote metric store. This method will never block and never throw an expected exception.
     */
    @NonBlocking
    void write(@NotNull Metric metric);

}
