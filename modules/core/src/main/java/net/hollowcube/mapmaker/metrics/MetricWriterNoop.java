package net.hollowcube.mapmaker.metrics;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MetricWriterNoop implements MetricWriter {
    private static final Logger logger = LoggerFactory.getLogger(MetricWriterNoop.class);

    public static final MetricWriter INSTANCE = new MetricWriterNoop();

    @Override
    public void write(@NotNull Metric metric) {
        logger.info("new metric {}: {}", metric.getClass().getSimpleName(), metric);
    }
}
