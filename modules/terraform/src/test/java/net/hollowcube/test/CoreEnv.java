package net.hollowcube.test;

import net.hollowcube.test.snapshot.Snapshot;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import static net.minestom.server.network.NetworkBuffer.BYTE_ARRAY;
import static net.minestom.server.network.NetworkBuffer.STRING;

@SuppressWarnings("UnstableApiUsage")
final class CoreEnv {
    private static final URI VIEWER_URL = URI.create("http://localhost:12415/");
    private static final HttpClient httpClient = HttpClient.newHttpClient();

    private final Map<Class<?>, Object> snapshots = new ConcurrentHashMap<>();
    private final List<SnapshotFailure> snapshotFailures = new CopyOnWriteArrayList<>();

    record SnapshotFailure(String id, byte[] expected, byte[] actual) {
    }

    private final Path workspaceRoot;

    private final boolean hasSnapshotViewer;

    public CoreEnv() {
        workspaceRoot = System.getenv("BUILD_WORKSPACE_DIRECTORY") != null
                ? Path.of(System.getenv("BUILD_WORKSPACE_DIRECTORY")) : null;

        var hasSnapshotViewer = false;
        try {
            var res = httpClient.send(
                    HttpRequest.newBuilder(VIEWER_URL).build(),
                    HttpResponse.BodyHandlers.discarding()
            );
            hasSnapshotViewer = res.statusCode() == 200;
        } catch (Exception e) {
            // ignore
        }
        this.hasSnapshotViewer = hasSnapshotViewer;
    }

    public boolean isUpdatingSnapshots() {
        return workspaceRoot != null;
    }

    public @NotNull Path getResourcePathOnDisk() {
        Check.stateCondition(!isUpdatingSnapshots(), "Not updating snapshots");
        return workspaceRoot.resolve("modules/terraform/src/test/resources");
    }

    public <T, S> @NotNull Snapshot<T, S> getSnapshot(@NotNull Class<? extends Snapshot<T, S>> snapshotType) {
        return (Snapshot<T, S>) snapshots.computeIfAbsent(snapshotType, t -> {
            try {
                return snapshotType.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        });
    }

    public <T, S> void reportFailedSnapshot(@NotNull Snapshot<T, S> snapshot, @NotNull S expected, @NotNull S actual) {
        snapshotFailures.add(new SnapshotFailure(
                "instance",
                snapshot.serializeSnapshot(expected),
                snapshot.serializeSnapshot(actual)));
    }

    public void afterAllHook() {
        if (!hasSnapshotViewer || snapshotFailures.isEmpty()) return;

        try {
            var data = NetworkBuffer.makeArray(buffer -> {
                buffer.writeCollection(snapshotFailures, (unused, failure) -> {
                    buffer.write(STRING, failure.id());
                    buffer.write(BYTE_ARRAY, failure.expected());
                    buffer.write(BYTE_ARRAY, failure.actual());
                });
            });

            httpClient.send(
                    HttpRequest.newBuilder(VIEWER_URL.resolve("report"))
                            .method("POST", HttpRequest.BodyPublishers.ofByteArray(data))
                            .build(),
                    HttpResponse.BodyHandlers.discarding()
            );
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
