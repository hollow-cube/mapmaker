package net.hollowcube.world.storage;

import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.util.Map;

public interface FileStorage {

    /**
     * Uploads the given data to the storage under the given path. Returns the unique identifier of the file.
     * Not necessarily the same as the path.
     */
    default @Blocking
    @NotNull String uploadFile(@NotNull String path, @NotNull InputStream data, long size) {
        return uploadFile(path, data, size, Map.of());
    }

    @Blocking
    @NotNull String uploadFile(@NotNull String path, @NotNull InputStream data, long size, @NotNull Map<String, String> metadata);

    @Blocking
    @NotNull StoredFile downloadFile(@NotNull String path);

}
