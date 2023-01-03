package net.hollowcube.mapmaker.storage;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.mapmaker.metrics.Metric;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class MetricStorageMongo implements MetricStorage {
    private static final String DB_NAME = System.getProperty("mongo.db", "mapmaker");

    private final MongoClient client;

    public MetricStorageMongo(@NotNull MongoClient client) {
        this.client = client;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> addMetric(@NotNull Metric metric) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                collection().insertOne(metric);
            } catch (DuplicateKeyException ignored) {
                throw DUPLICATE_ENTRY;
            }
            return true;
        });
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> updateMetric(@NotNull Metric metric) {
        return null;
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Double> getValue(@NotNull int id, @NotNull String source, @NotNull String target) {
        return null;
    }

    private @NotNull MongoCollection<Metric> collection() {
        return client.getDatabase(DB_NAME).getCollection("metrics", Metric.class);
    }
}
