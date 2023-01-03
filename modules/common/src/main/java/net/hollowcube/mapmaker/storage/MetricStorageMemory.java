package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.metrics.Metric;
import net.hollowcube.mapmaker.metrics.MetricsHelper;
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
        LOGGER.info("Adding metric {}", MetricsHelper.getMetricString(metric.id));
        metrics.add(metric);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> updateMetric(@NotNull Metric metric) {
        LOGGER.info("Updating metric {}", MetricsHelper.getMetricString(metric.id));
        try {
            Optional<Metric> matchingMetric = metrics.stream().filter(cachedMetric -> (
                    (metric.id == cachedMetric.id) && (metric.source == cachedMetric.source) && (metric.target == cachedMetric.target)
            )).findFirst();
            metrics.remove(matchingMetric);
            metrics.add(metric);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public @NotNull CompletableFuture<@NotNull Double> getValue(@NotNull int id, @NotNull String source, @NotNull String target) {
        try {
            Optional<Metric> matchingMetric = metrics.stream().filter(cachedMetric -> (
                    (id == cachedMetric.id) && (source == cachedMetric.source) && (target == cachedMetric.target)
                    )).findFirst();
            return CompletableFuture.completedFuture(matchingMetric.get().value);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
