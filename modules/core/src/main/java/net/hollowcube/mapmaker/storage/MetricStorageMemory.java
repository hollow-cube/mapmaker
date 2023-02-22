package net.hollowcube.mapmaker.storage;

import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.metrics.Metric;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

class MetricStorageMemory implements MetricStorage {
    public static final Logger logger = LoggerFactory.getLogger(MetricStorageMemory.class);

    private final Set<Metric> metrics = new HashSet<>();

    @Override
    public @NotNull FutureResult<@NotNull Metric> addMetric(@NotNull Metric metric) {
        logger.info("Adding metric ", metric.asString());
        metrics.add(metric);
        return FutureResult.of(metric);
    }
}
