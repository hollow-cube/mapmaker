package net.hollowcube.mapmaker.metrics;

import net.hollowcube.mapmaker.storage.MetricStorage;

import java.time.Instant;

public class MetricsHelper {
    private enum MetricsEnum {
        USER_SESSION_PLAY_TIME_MS("USER_SESSION_PLAY_TIME_MS"),
        USER_FIRST_JOIN_TIME_MS("USER_FIRST_JOIN_TIME_MS"),
        // WARNING: When adding new metrics, only append them to the end of this enum
        // Never update an existing metric (unless functionality is broken).
        // When creating a metric, add as much information as reasonable rather than
        // creating multiple metrics containing the same info.
        ;

        private final String name;

        MetricsEnum(String name) {
            this.name = name;
        }
    }

    private static MetricsHelper instance = null;
    private static MetricStorage storage = null;

    public static MetricsHelper init(MetricStorage metricStorage) {
        if (instance != null) {
            return instance;
        }
        instance = new MetricsHelper();
        storage = metricStorage;
        return instance;
    }

    public static MetricsHelper get() {
        return instance;
    }

    public String getMetricName(int tag) {
        if (tag < MetricsEnum.values().length) {
            return MetricsEnum.values()[tag].name;
        }
        else {
            return "NAN";
        }
    }

    public int getMetricTag(String name) {
        try {
            var metricEnum = MetricsEnum.valueOf(name);
            return metricEnum.ordinal();
        } catch (IllegalArgumentException i) {
            return -1;
        }
    }

/**
 * Record metric helper functions. These are what will be called in code to update a metric.
 */

    /**
     * Records a new metric for a player's first join time
     * @param uuid Player UUID
     */
    public void recordMetricFirstJoinTime(String uuid) {
        Metric metric = new Metric(
                MetricsEnum.USER_FIRST_JOIN_TIME_MS.name,
                Instant.now().toEpochMilli(),
                uuid,
                Instant.now().toEpochMilli()
        );
        storage.addMetric(metric);
    }

    /**
     * Records a new metric for player's session play time
     * @param uuid Player UUID
     * @param timestamp_ms Time since epoch in milliseconds
     */
    public void recordMetricSessionPlayTimeMs(String uuid, long timestamp_ms) {
        Metric metric = new Metric(
                MetricsEnum.USER_SESSION_PLAY_TIME_MS.name,
                Instant.now().toEpochMilli(),
                uuid,
                timestamp_ms
        );
        storage.addMetric(metric);
    }
}
