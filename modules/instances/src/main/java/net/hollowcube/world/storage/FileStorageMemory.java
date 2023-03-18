package net.hollowcube.world.storage;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class FileStorageMemory implements FileStorage {
    private record MemoryFile(byte[] data, long size, @NotNull Map<String, String> metadata) {
        public @NotNull StoredFile toStoredFile() {
            return new StoredFile(new ByteArrayInputStream(data), size, metadata);
        }
    }

    private final Map<String, MemoryFile> files = new ConcurrentHashMap<>();

    @Override
    public @NotNull CompletableFuture<@NotNull String> uploadFile(@NotNull String path, @NotNull InputStream data, long size, @NotNull Map<String, String> userMetadata) {
        try {
            files.put(path, new MemoryFile(data.readAllBytes(), size, userMetadata));
            return CompletableFuture.completedFuture(path);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    @Override
    public @NotNull CompletableFuture<@NotNull StoredFile> downloadFile(@NotNull String path) {
        if (!files.containsKey(path))
            return CompletableFuture.failedFuture(new Exception("File not found"));
        return CompletableFuture.completedFuture(files.get(path).toStoredFile());
    }
}
