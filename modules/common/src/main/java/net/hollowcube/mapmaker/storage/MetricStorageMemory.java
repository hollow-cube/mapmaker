package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.metrics.Metric;
import net.hollowcube.mapmaker.metrics.MetricsHelper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MetricStorageMemory implements MetricStorage {
    public static final Logger LOGGER = LoggerFactory.getLogger(MetricStorageMemory.class);

    private static Set<Metric> metrics;

    @Override
    public @NotNull CompletableFuture<@NotNull Boolean> addMetric(@NotNull Metric metric) {
        LOGGER.info("Adding metric {}", MetricsHelper.getMetricName(metric.getTag()));
        metrics.add(metric);
        return CompletableFuture.completedFuture(true);
    }

    @Override
    public @NotNull CompletableFuture<Metric> updateMetric(
            @NotNull Metric metric, @NotNull int[] match_indices, @NotNull int[] update_indices) {
        LOGGER.info("Updating metric {}", MetricsHelper.getMetricName(metric.getTag()));
        try {
            Optional<Metric> matchingMetric = metrics.stream().filter(cachedMetric -> (
                    (metric.getTag() == cachedMetric.getTag()) &&
                    (metric.getValues(match_indices).equals(cachedMetric.getValues(match_indices)))
            )).findFirst();
            Metric updateMetric = matchingMetric.get();
            if (!matchingMetric.isEmpty()) {
                metrics.remove(matchingMetric.get());
                for (int idx : update_indices) {
                    updateMetric.setValue(idx, metric.getValue(idx));
                }
            } else {
                updateMetric = metric;
            }
            metrics.add(updateMetric);
            return CompletableFuture.completedFuture(matchingMetric.get());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public @NotNull CompletableFuture<Set<Metric>> getMatchingMetrics(@NotNull int tag, @NotNull ArrayList match_values, @NotNull int[] match_indices) {
        try {
            Set<Metric> matchingMetrics = metrics.stream().filter(cachedMetric -> (
                    (tag == cachedMetric.getTag()) &&
                    (match_values.equals(cachedMetric.getValues(match_indices)))
                    )).collect(Collectors.toSet());
            return CompletableFuture.completedFuture(matchingMetrics);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public CompletableFuture<Set<Metric>> getCachedMetrics() {
        return CompletableFuture.completedFuture(metrics);
    }
}
