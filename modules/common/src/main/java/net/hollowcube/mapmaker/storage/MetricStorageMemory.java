package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.metrics.Metric;
import net.hollowcube.mapmaker.metrics.MetricsHelper;
import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public class MetricStorageMemory implements MetricStorage {
    public static final Logger LOGGER = LoggerFactory.getLogger(MetricStorageMemory.class);

    private static Set<Metric> metrics;

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> addMetric(@NotNull Metric metric) {
        LOGGER.info("Adding metric {}", MetricsHelper.getMetricString(metric.getId()));
        metrics.add(metric);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public @NotNull CompletableFuture<Metric> updateMetric(@NotNull Metric metric) {
        LOGGER.info("Updating metric {}", MetricsHelper.getMetricString(metric.getId()));
        try {
            Optional<Metric> matchingMetric = metrics.stream().filter(cachedMetric -> (
                    (metric.getId() == cachedMetric.getId()) &&
                    (metric.getSource() == cachedMetric.getSource()) &&
                    (metric.getTarget() == cachedMetric.getTarget())
            )).findFirst();
            if (!matchingMetric.isEmpty())
                metrics.remove(matchingMetric.get());
            metrics.add(metric);
            return CompletableFuture.completedFuture(matchingMetric.get());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public CompletableFuture<Double> getValue(@NotNull int id, @NotNull String source, @NotNull String target) {
        try {
            Optional<Metric> matchingMetric = metrics.stream().filter(cachedMetric -> (
                    (id == cachedMetric.getId()) &&
                    (source == cachedMetric.getSource()) &&
                    (target == cachedMetric.getTarget())
                    )).findFirst();
            return CompletableFuture.completedFuture(matchingMetric.get().getValue());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public CompletableFuture<Set<Metric>> getCachedMetrics() {
        return CompletableFuture.completedFuture(metrics);
    }
}
