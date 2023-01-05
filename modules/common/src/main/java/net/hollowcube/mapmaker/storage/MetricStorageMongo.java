package net.hollowcube.mapmaker.storage;

import com.mongodb.DuplicateKeyException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import net.hollowcube.mapmaker.metrics.Metric;
import net.hollowcube.mapmaker.metrics.MetricsHelper;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import static com.mongodb.client.model.Filters.*;

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
    public @NotNull CompletableFuture<Metric> updateMetric(@NotNull Metric metric, @NotNull int[] match_indices, @NotNull int[] update_indices) {
        return CompletableFuture.supplyAsync(() -> {
            var filter = and(
                    eq("tag", metric.getTag()),
                    all("values", metric.getValues(match_indices)));
            var matchingMetric = collection().findOneAndDelete(filter);
            Metric updateMetric = matchingMetric;
            for (int idx : update_indices) {
                updateMetric.setValue(idx, metric.getValue(idx));
            }
            collection().insertOne(updateMetric);
            return matchingMetric;
        }, ForkJoinPool.commonPool());
    }

    @Override
    public @NotNull CompletableFuture<Set<Metric>> getMatchingMetrics(@NotNull int tag, @NotNull ArrayList match_values, @NotNull int[] match_indices) {
        return CompletableFuture.supplyAsync(() -> {
            var filter = and(
                    eq("tag", tag),
                    all("values", match_values));
            var matchingMetrics = collection().find(filter).into(new ArrayList<Metric>());
            return (Set<Metric>) matchingMetrics;
        }, ForkJoinPool.commonPool());
    }

    private @NotNull MongoCollection<Metric> collection() {
        return client.getDatabase(DB_NAME).getCollection("metrics", Metric.class);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> syncMetrics() throws ExecutionException, InterruptedException {
        var cachedMetrics = cachedStorage.getCachedMetrics().get();
        for (Metric metric : cachedMetrics) {
            CompletableFuture<?> completableFuture =
                    metric.isUnique() ?
                        updateMetric(metric, metric.getMatchIndices(), metric.getUpdateIndices()) :
                        addMetric(metric);
            cachedMetrics.remove(metric);
        }
        return CompletableFuture.completedFuture(true);
    }
}
