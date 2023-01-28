package net.hollowcube.mapmaker.dev.temp;

import net.hollowcube.world.storage.FileStorage;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TODO: move me to the common module
 */
public class FileStorageMemory implements FileStorage {
    private final Map<String, byte[]> files = new ConcurrentHashMap<>();

    @Override
    public @NotNull CompletableFuture<@NotNull String> uploadFile(@NotNull String path, @NotNull InputStream data, int size) {
        try {
            files.put(path, data.readAllBytes());
            return CompletableFuture.completedFuture(path);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public @NotNull CompletableFuture<InputStream> downloadFile(@NotNull String path) {
        if (!files.containsKey(path))
            return CompletableFuture.failedFuture(new Exception("File not found"));
        return CompletableFuture.completedFuture(new ByteArrayInputStream(files.get(path)));
    }
}
