package net.hollowcube.mapmaker.storage;

import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.mapmaker.metrics.Metric;
import net.hollowcube.mapmaker.storage.client.MongoClientFactory;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

public interface MetricStorage {

    static @NotNull MetricStorage memory() {
        return new MetricStorageMemory();
    }

    @Blocking
    static @NotNull MetricStorage mongo(@NotNull MongoConfig config) {
        var client = MongoClientFactory.get().newClient(config);
        return new MetricStorageMongo(client, config);
    }

    @Blocking
    @NotNull Metric addMetric(@NotNull Metric metric);
}
