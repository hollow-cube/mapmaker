package net.hollowcube.mapmaker.storage;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public interface MetricStorageDB extends MetricStorage {

    /**
     * Syncs metrics from the memory implementation of storage to the corresponding database.
     * @return
     */
    @NotNull CompletableFuture<@NotNull Boolean> syncMetrics() throws ExecutionException, InterruptedException;
}
