package net.hollowcube.mapmaker.storage;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.common.config.MongoConfig;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.common.result.Result;
import net.hollowcube.mapmaker.metrics.Metric;
import org.jetbrains.annotations.NotNull;

class MetricStorageMongo implements MetricStorage {
    private final MongoClient client;
    private final MongoConfig config;

    public MetricStorageMongo(@NotNull MongoClient client, @NotNull MongoConfig config) {
        this.client = client;
        this.config = config;
    }

    @Override
    public @NotNull FutureResult<@NotNull Metric> addMetric(@NotNull Metric metric) {
        return FutureResult.supply(() -> {
            try {
                collection().insertOne(metric);
            } catch (DuplicateKeyException ignored) { }
            return Result.of(metric);
        });
    }

    private @NotNull MongoCollection<Metric> collection() {
        return client.getDatabase(config.database()).getCollection("metrics", Metric.class);
    }
}
