package net.hollowcube.mapmaker.metrics;

import net.hollowcube.mapmaker.storage.MetricStorageDB;
import net.hollowcube.mapmaker.storage.MetricStorageMemory;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class MetricsHelper {
    private enum MetricsEnum {
        USER_TOTAL_PLAY_TIME_MS("USER_TOTAL_PLAY_TIME_MS", 2, new int[]{0}, new int[]{1}),
        USER_SESSION_PLAY_TIME_MS("USER_SESSION_PLAY_TIME_MS", 2),
        USER_JUMP_COUNT("USER_JUMP_COUNT", 2, new int[]{0}, new int[]{1}),
        USER_FIRST_JOIN_TIME_MS("USER_FIRST_JOIN_TIME_MS", 2, new int[]{0}, new int[]{1}),
        // WARNING: When adding new metrics, only append them to the end of this enum
        ;

        private final String name;

        /**
         * The number of values associated with a metric, besides its tag and timestamp.
         *
         * Ex:
         * USER_JUMP_COUNT: 2 -> UUID and JUMP_COUNT
         */
        private final int value_count;

        /**
         * Determines if a metric is unique or not. Determine "uniqueness" when defining a metric,
         * based on if there should only be one per user/map etc or if it will be repeating.
         *
         * Ex:
         * USER_JUMP_COUNT: Unique , since each player has one ongoing jump count
         * USER_SESSION_PLAY_TIME_MS: Not Unique, since each player has multiple sessions with different play times
         */
        private final boolean unique;

        /**
         * The indices of values used to match against existing metrics of the same type
         * (usually some type of primary key like UUID).
         *
         * NOTE: Only needed for UNIQUE metrics (unique flag = true).
         *
         * Ex:
         * USER_SESSION_PLAY_TIME_MS: [0] -> UUID
         */
        private final int[] match_indices;

        /**
         * The indices of values to update when updating a matching metric.
         *
         * NOTE: Only needed for UNIQUE metrics (unique flag = true).
         *
         * Ex:
         * USER_SESSION_PLAY_TIME_MS: [1] -> timestamp_ms
         */
        private final int[] update_indices;

        // Non-unique metric constructor
        MetricsEnum(String name, int value_count) {
            this.name = name;
            this.value_count = value_count;
            this.unique = false;
            this.match_indices = new int[]{};
            this.update_indices = new int[]{};
        }

        // Unique metric constructor
        MetricsEnum(String name, int value_count, int[] match_indices, int[] update_indices) {
            this.name = name;
            this.value_count = value_count;
            this.unique = true;
            this.match_indices = match_indices;
            this.update_indices = update_indices;
        }
    };

    static private MetricStorageMemory cachedStorage;
    static private MetricStorageDB dbStorage;

    private MetricsHelper(MetricStorageMemory cachedStorage, MetricStorageDB dbStorage) {
        this.cachedStorage = cachedStorage;
        this.dbStorage = dbStorage;
    }

    public static String getMetricName(int tag) {
        if (tag < MetricsEnum.values().length)
            return MetricsEnum.values()[tag].name;
        else
            return "NAN";
    }

    public static int getMetricValueCount(int tag) {
        if (tag < MetricsEnum.values().length)
            return MetricsEnum.values()[tag].value_count;
        else
            return -1;
    }

    public static int getMetricId(String name) {
        return MetricsEnum.valueOf(name).ordinal();
    }

    public static boolean isUnique(int tag) {
        return MetricsEnum.values()[tag].unique;
    }

    public static int[] getMatchIndices(int tag) {
        return MetricsEnum.values()[tag].match_indices;
    }

    public static int[] getUpdateIndices(int tag) {
        return MetricsEnum.values()[tag].update_indices;
    }

/**
 * Record metric helper functions.
 * Note that for metrics which will be called infrequently, they can be sent
 * directly to the database (like first join, total play time, session play time, etc)
 * while metrics which are called very frequently should be cached (jump count, etc).
 */

    /**
     * Records a new metric for player's first join time
     * @param uuid Player UUID
     * @param timestamp_ms Time since epoch in milliseconds
     */
    public static void recordMetricFirstJoinTime(String uuid, long timestamp_ms) {
        Metric metric = new Metric(
                MetricsEnum.USER_FIRST_JOIN_TIME_MS.ordinal(),
                Instant.now().toEpochMilli(),
                uuid,
                timestamp_ms);
        dbStorage.updateMetric(
                metric,
                MetricsEnum.USER_FIRST_JOIN_TIME_MS.match_indices,
                MetricsEnum.USER_FIRST_JOIN_TIME_MS.update_indices);
    }

    /**
     * Updates player's jump count metric by one
     * @param uuid Player UUID
     */
    public static void recordMetricJumpCount(String uuid) throws ExecutionException, InterruptedException {
        int jump_count = 1;
        int id = MetricsEnum.USER_JUMP_COUNT.ordinal();
        int[] match_indices = MetricsEnum.USER_JUMP_COUNT.match_indices;
        int[] update_indices = MetricsEnum.USER_JUMP_COUNT.update_indices;
        int jump_idx = MetricsEnum.USER_JUMP_COUNT.update_indices[0];
        Set<Metric> userJumpMetric = cachedStorage.getMatchingMetrics(id, new ArrayList(), match_indices).get();
        if (userJumpMetric.isEmpty()) {
            userJumpMetric = dbStorage.getMatchingMetrics(id, new ArrayList(), match_indices).get();
            if (!userJumpMetric.isEmpty()) {
                jump_count = (int) userJumpMetric.iterator().next().getValue(jump_idx) + 1;
            }
        } else {
            jump_count = (int) userJumpMetric.iterator().next().getValue(jump_idx) + 1;
        }
        Metric metric = new Metric(
                id,
                Instant.now().toEpochMilli(),
                uuid,
                jump_count);
        cachedStorage.updateMetric(metric, match_indices, update_indices);
    }

    /**
     * Records a new metric for player's total play time
     * @param uuid Player UUID
     * @param timestamp_ms Time since epoch in milliseconds
     */
    public static void recordMetricTotalPlayTimeMs(String uuid, long timestamp_ms) {
        Metric metric = new Metric(
                MetricsEnum.USER_TOTAL_PLAY_TIME_MS.ordinal(),
                Instant.now().toEpochMilli(),
                uuid,
                timestamp_ms);
        dbStorage.updateMetric(
                metric,
                MetricsEnum.USER_TOTAL_PLAY_TIME_MS.match_indices,
                MetricsEnum.USER_TOTAL_PLAY_TIME_MS.update_indices);
    }

    /**
     * Records a new metric for player's session play time
     * @param uuid Player UUID
     * @param timestamp_ms Time since epoch in milliseconds
     */
    public static void recordMetricSessionPlayTimeMs(String uuid, long timestamp_ms) {
        Metric metric = new Metric(
                MetricsEnum.USER_SESSION_PLAY_TIME_MS.ordinal(),
                Instant.now().toEpochMilli(),
                uuid,
                timestamp_ms);
        cachedStorage.addMetric(metric);
    }
}