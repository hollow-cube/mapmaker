package net.hollowcube.mapmaker.metrics;

import net.hollowcube.mapmaker.storage.MetricStorage;
import net.hollowcube.mapmaker.storage.MetricStorageDB;
import net.hollowcube.mapmaker.storage.MetricStorageMemory;
import net.hollowcube.mapmaker.storage.MetricStorageMongo;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public final class MetricsHelper {
    private enum MetricsEnum {
        USER_TOTAL_PLAY_TIME_MS("USER_TOTAL_PLAY_TIME_MS"),
        USER_SESSION_PLAY_TIME_MS("USER_SESSION_PLAY_TIME_MS"),
        USER_JUMP_COUNT("USER_JUMP_COUNT"),
        USER_FIRST_JOIN_TIME_MS("USER_FIRST_JOIN_TIME_MS"),
        // WARNING: When adding new metrics, only append them to the end of this enum
        ;

        private final String text;

        MetricsEnum(String text) {
            this.text = text;
        }

        public static boolean isUnique(MetricsEnum metric) {
            switch (metric) {
                case USER_SESSION_PLAY_TIME_MS:
                    return false;
                case USER_TOTAL_PLAY_TIME_MS:
                case USER_JUMP_COUNT:
                case USER_FIRST_JOIN_TIME_MS:
                default:
                    return true;
            }
        }
    };

    static private MetricStorageMemory cachedStorage;
    static private MetricStorageDB dbStorage;

    private MetricsHelper(MetricStorageMemory cachedStorage, MetricStorageDB dbStorage) {
        this.cachedStorage = cachedStorage;
        this.dbStorage = dbStorage;
    }

    public static String getMetricString(int id) {
        if (id < MetricsEnum.values().length)
            return MetricsEnum.values()[id].text;
        else
            return "NAN";
    }

    public static int getMetricId(String name) {
        return MetricsEnum.valueOf(name).ordinal();
    }

    /**
     * Determines if a metric is unique or not. Determine "uniqueness" when defining a metric,
     * based on if there should only be one per user/map etc or if it will be repeating.
     *
     * Ex:
     * USER_JUMP_COUNT: Unique , since each player has one ongoing jump count
     * USER_SESSION_PLAY_TIME_MS: Not Unique, since each player has multiple sessions with different play times
     *
     * @param id Metric ID
     * @return True if unique, False if not
     */
    public static boolean isUnique(int id) {
        return MetricsEnum.isUnique(MetricsEnum.values()[id]);
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
    public static void recordMetricFirstJoinTime(String uuid, double timestamp_ms) {
        Metric metric = new Metric(MetricsEnum.USER_FIRST_JOIN_TIME_MS.ordinal(), uuid, "", timestamp_ms);
        dbStorage.addMetric(metric);
    }

    /**
     * Updates player's jump count metric by one
     * @param uuid Player UUID
     */
    public static void recordMetricJumpCount(String uuid) throws ExecutionException, InterruptedException {
        int id = MetricsEnum.USER_JUMP_COUNT.ordinal();
        CompletableFuture<Double> jumps;
        if ((jumps = cachedStorage.getValue(id, uuid, "")).get().isNaN()) {
            if ((jumps = dbStorage.getValue(id, uuid, "")).get().isNaN()) {
                System.out.println("Could not find existing jump count for player " + uuid);
                return;
            }
        }
        Metric metric = new Metric(id, uuid, "", jumps.get().intValue() + 1);
        cachedStorage.addMetric(metric);
    }

    /**
     * Records a new metric for player's total play time
     * @param uuid Player UUID
     * @param timestamp_ms Time since epoch in milliseconds
     */
    public static void recordMetricTotalPlayTimeMs(String uuid, double timestamp_ms) {
        Metric metric = new Metric(MetricsEnum.USER_TOTAL_PLAY_TIME_MS.ordinal(), uuid, "", timestamp_ms);
        dbStorage.addMetric(metric);
    }

    /**
     * Records a new metric for player's session play time
     * @param uuid Player UUID
     * @param timestamp_ms Time since epoch in milliseconds
     */
    public static void recordMetricSessionPlayTimeMs(String uuid, double timestamp_ms) {
        Metric metric = new Metric(MetricsEnum.USER_SESSION_PLAY_TIME_MS.ordinal(), uuid, "", timestamp_ms);
        cachedStorage.addMetric(metric);
    }
}