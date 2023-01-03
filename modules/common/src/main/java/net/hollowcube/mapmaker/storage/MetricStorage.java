package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.metrics.Metric;
import net.hollowcube.mapmaker.util.MongoUtil;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public interface MetricStorage extends Storage {

    static @NotNull MetricStorage memory() {
        return new MetricStorageMemory();
    }

    static @NotNull MetricStorage mongo(@NotNull String uri) {
        return new MetricStorageMongo(MongoUtil.getClient(uri));
    }

    /**
     * Adds a new metric via insertion, does not overwrite metric with matching id, source, and target
     * @param metric
     * @return
     */
    @NotNull CompletableFuture<@NotNull Boolean> addMetric(@NotNull Metric metric);

    /**
     * Adds a new metric via insertion, overwrites metric with matching id, source, and target
     * @param metric
     * @return
     */
    @NotNull CompletableFuture<@NotNull Boolean> updateMetric(@NotNull Metric metric);

    /**
     * Gets the existing value of a metric with matching id, source, and target
     * @param id
     * @param source
     * @param target
     * @return Double value if exists, null if does not exist
     */
    @NotNull CompletableFuture<@NotNull Double> getValue(@NotNull int id, @NotNull String source, @NotNull String target);
}
