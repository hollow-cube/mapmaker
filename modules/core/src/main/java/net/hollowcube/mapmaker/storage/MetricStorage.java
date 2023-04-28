package net.hollowcube.mapmaker.storage;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.common.result.FutureResult;
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

    @NotNull FutureResult<@NotNull Metric> addMetric(@NotNull Metric metric);
}
