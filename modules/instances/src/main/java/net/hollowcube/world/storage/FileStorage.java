package net.hollowcube.world.storage;

import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.concurrent.CompletableFuture;

public interface FileStorage {

    /**
     * Uploads the given data to the storage under the given path. Returns the unique identifier of the file.
     * Not necessarily the same as the path.
     */
    @NotNull CompletableFuture<@NotNull String> uploadFile(@NotNull String path, @NotNull InputStream data, int size);

//    @NotNull CompletableFuture<Void> updateFile(@NotNull String path, @NotNull InputStream data, int size);

    @NotNull CompletableFuture<InputStream> downloadFile(@NotNull String path);

}
