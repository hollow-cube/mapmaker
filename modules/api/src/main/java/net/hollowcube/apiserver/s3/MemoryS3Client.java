package net.hollowcube.apiserver.s3;

import net.hollowcube.ipc.Blob;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// The bucket as a map.
///
/// [#puts] is counted apart from the map's size because what most tests are about is whether a
/// rejected or retried write transferred anything at all, and an overwrite leaves the size
/// unchanged.
public final class MemoryS3Client implements S3Client {

    private final Map<String, byte[]> objects = new LinkedHashMap<>();
    private int puts;
    private @Nullable Runnable afterPut;

    public Map<String, byte[]> objects() {
        return objects;
    }

    public int puts() {
        return puts;
    }

    /// Runs once, on the next [#put], after the object is stored — the only way to open the window
    /// between staging an object and taking the row lock from a single-threaded test.
    @TestOnly
    public void afterPut(Runnable action) {
        afterPut = action;
    }

    @Override
    public void put(String key, InputStream body, long length) {
        puts++;
        try {
            var bytes = body.readAllBytes();
            if (length >= 0 && bytes.length != length)
                throw new IllegalStateException("announced " + length + " bytes but wrote " + bytes.length);
            objects.put(key, bytes);
            var action = afterPut;
            afterPut = null;
            if (action != null) action.run();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public Blob get(String key) {
        var object = objects.get(key);
        if (object == null) throw new NotFoundError(key);
        return Blob.of(object);
    }

    @Override
    public Blob getRange(String key, long start, long endInclusive) {
        var object = objects.get(key);
        if (object == null) throw new NotFoundError(key);
        return Blob.of(Arrays.copyOfRange(object, (int) start, (int) Math.min(endInclusive + 1, object.length)));
    }

    @Override
    public void delete(String key) {
        objects.remove(key);
    }

    @Override
    public List<String> list(String prefix) {
        return objects.keySet().stream().filter(key -> key.startsWith(prefix)).sorted().toList();
    }
}
