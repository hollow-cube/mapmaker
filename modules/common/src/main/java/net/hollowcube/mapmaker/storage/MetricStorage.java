package net.hollowcube.mapmaker.storage;

import net.hollowcube.mapmaker.metrics.Metric;
import net.hollowcube.mapmaker.util.MongoUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface MetricStorage extends Storage {

    static @NotNull MetricStorage memory() {
        return new MetricStorageMemory();
    }

    static @NotNull MetricStorage mongo(@NotNull String uri, @NotNull MetricStorageMemory cachedStorage) {
        return new MetricStorageMongo(MongoUtil.getClient(uri), cachedStorage);
    }

    /**
     * Adds a new metric via insertion, does not overwrite duplicates
     * NOTE: Best used for NON-UNIQUE metrics
     * @param metric The metric to add
     * @return
     */
    @NotNull CompletableFuture addMetric(@NotNull Metric metric);

    /**
     * Adds a new metric via insertion, overwrites existing metric matching at match_indices
     * at the specified update_indices
     * NOTE: Best used for UNIQUE metrics
     *
     * Ex: updateMetric(
     *          Metric(USER_FIRST_JOIN_TIME_MS, UUID_ABC, TIME_123),
     *          [0],
     *          [1]
     *     ) -> Updates metric with matching UUID to TIME_123, replacing old metric for this user.
     *
     * @param metric The metric to update
     * @param match_indices List of value indices to match on (indices with reference to the order of those in storage)
     * @param update_indices List of value indices to update for first metric which matches on the match_indices
     * @return Replaced metric with old value before update, null if did not exist prior
     */
    @NotNull CompletableFuture<Metric> updateMetric(
            @NotNull Metric metric, @NotNull int[] match_indices, @NotNull int[] update_indices);

    /**
     * Gets a set of metrics with matching tag and provided values on the list of indices
     *
     * Ex: Storage {
     *          Metric(USER_JUMP_COUNT, UUID_ABC, COUNT_123),
     *          Metric(USER_JUMP_COUNT, UUID_DEF, COUNT_456),
     *          Metric(USER_JUMP_COUNT, UUID_GHI, COUNT_456)
     *     }
     *
     *     getMetrics(USER_JUMP_COUNT, [], [])
     *          -> returns all metrics with tag USER_JUMP_COUNT
     *
     *     getMetrics(USER_JUMP_COUNT, [UUID_DEF], [0])
     *          -> returns [Metric(USER_JUMP_COUNT, UUID_DEF, COUNT_456)]
     *
     *     getMetrics(USER_JUMP_COUNT, [COUNT_456], [1])
     *          -> returns [
     *                          Metric(USER_JUMP_COUNT, UUID_DEF, COUNT_456),
     *                          Metric(USER_JUMP_COUNT, UUID_GHI, COUNT_456)
     *                     ]
     *
     * @param tag Type of metric to get values from
     * @param match_indices List of value indices to match on (indices with reference to the order of those in storage)
     * @param match_values List of values to match on (sizeof(match_indices) = sizeof(match_values))
     * @return Set of metrics that match requirements, empty set if none match
     */
    @NotNull CompletableFuture<Set<Metric>> getMatchingMetrics(
            @NotNull int tag, @NotNull ArrayList match_values, @NotNull int[] match_indices);
}
