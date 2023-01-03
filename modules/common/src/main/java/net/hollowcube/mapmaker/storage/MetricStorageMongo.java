package net.hollowcube.mapmaker.storage;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.mapmaker.metrics.Metric;
import net.hollowcube.mapmaker.metrics.MetricsHelper;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class MetricStorageMongo implements MetricStorageDB {
    private static final String DB_NAME = System.getProperty("mongo.db", "mapmaker");

    private final MongoClient client;
    private final MetricStorageMemory cachedStorage;

    public MetricStorageMongo(@NotNull MongoClient client, @NotNull MetricStorageMemory cachedStorage) {
        this.client = client;
        this.cachedStorage = cachedStorage;

        // Register callback to sync metrics
        Runnable syncMetrics = () -> {
            try {
                syncMetrics();
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        };
        TaskSchedule delay = TaskSchedule.minutes(5);
        TaskSchedule repeat = TaskSchedule.minutes(10);
        MinecraftServer.getSchedulerManager().scheduleTask(syncMetrics, delay, repeat);
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
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull CompletableFuture<Metric> updateMetric(@NotNull Metric metric) {
        return CompletableFuture.supplyAsync(() -> {
            var filter = and(
                    eq("id", metric.getId()),
                    eq("source", metric.getSource()),
                    eq("target", metric.getTarget()));
            var matchingMetric = collection().findOneAndReplace(filter, metric);
            return matchingMetric;
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Double> getValue(@NotNull int id, @NotNull String source, @NotNull String target) {
        return CompletableFuture.supplyAsync(() -> {
            var filter = and(
                    eq("id", id),
                    eq("source", source),
                    eq("target", target));
            var matchingMetric = collection().find(filter).limit(1).first();
            if (matchingMetric != null)
                return matchingMetric.getValue();
            else
                return null;
        });
    }

    private @NotNull MongoCollection<Metric> collection() {
        return client.getDatabase(DB_NAME).getCollection("metrics", Metric.class);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> syncMetrics() throws ExecutionException, InterruptedException {
        var cachedMetrics = cachedStorage.getCachedMetrics().get();
        for (Metric metric : cachedMetrics) {
            CompletableFuture<?> completableFuture =
                    MetricsHelper.isUnique(metric.getId()) ?
                    updateMetric(metric) :
                    addMetric(metric);
            cachedMetrics.remove(metric);
        }
        return CompletableFuture.completedFuture(true);
    }
}
