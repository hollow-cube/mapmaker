package net.hollowcube.test;

import net.hollowcube.test.snapshot.Snapshot;
import net.hollowcube.test.subject.TestConnection;
import net.hollowcube.test.subject.TestInstance;
import net.hollowcube.test.subject.TestPlayer;
import net.minestom.server.ServerProcess;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SuppressWarnings("UnstableApiUsage")
public class TestEnvImpl implements TestEnv {

    private final CoreEnv core;

    private final String uniqueTestId;
    private final String testId;
    private final ServerProcess process;

    private int instanceCounter = 0;

    public TestEnvImpl(@NotNull CoreEnv core, @NotNull String uniqueTestId, @NotNull String testId, @NotNull ServerProcess process) {
        this.core = core;

        this.uniqueTestId = uniqueTestId;
        this.testId = testId;
        this.process = process;
    }

    @NotNull CoreEnv core() {
        return core;
    }

    public @NotNull String getUniqueTestId() {
        return testId;
//        return testId + "-" + uniqueTestId.hashCode();
    }

    @Override
    public @NotNull ServerProcess process() {
        return process;
    }

    @Override
    public @NotNull TestInstance createEmptyInstance() {
        var instance = new TestInstance(this, String.format("%s-%d", uniqueTestId, instanceCounter++), null);
//        instance.setGenerator(unit -> unit.modifier().fillHeight(-20, 0, Block.GRASS_BLOCK));
        process.instance().registerInstance(instance);

        // Load some chunks around the center
        var loadingChunks = new ArrayList<CompletableFuture<Void>>();
        ChunkUtils.forChunksInRange(0, 0, 3, (x, z) ->
                loadingChunks.add(instance.loadChunk(x, z).thenApply(v -> null)));
        CompletableFuture.allOf(loadingChunks.toArray(CompletableFuture[]::new)).join();

        return instance;
    }

    @Override
    public @NotNull TestConnection createConnection() {
        return new TestConnection(this);
    }

    @Override
    public @NotNull TestPlayer createPlayer(@NotNull Instance instance, @NotNull Pos pos) {
        return createConnection().connect(instance, pos).join();
    }

    // Snapshots

    @Override
    public <T, S> void assertSnapshot(@NotNull Class<? extends Snapshot<T, S>> snapshotType, @NotNull T value) {
        var snapshot = core.getSnapshot(snapshotType);
        var actual = snapshot.createSnapshot(value);

        // If we are in writer mode, simply write the snapshot data to disk
        if (core.isUpdatingSnapshots()) try {
            var outDirectory = core.getResourcePathOnDisk().resolve("snapshots");
            Files.createDirectories(outDirectory);

            var serialized = snapshot.serializeSnapshot(actual);
            Files.write(outDirectory.resolve(getUniqueTestId()), serialized);
            return;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Load the snapshot from resources, notably not using the disk path because we do not
        // know it when running inside the Bazel sandbox.
        S expected;
        try (var snapshotInputStream = CoreEnv.class.getResourceAsStream("/snapshots/" + testId)) {
            assertNotNull(snapshotInputStream, "missing snapshot for " + testId);

            expected = snapshot.deserializeSnapshot(snapshotInputStream.readAllBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Compare the snapshots, reporting to the viewer if it is present.
        try {
            snapshot.assertSnapshot(expected, actual);
        } catch (Throwable e) {
            core.reportFailedSnapshot(snapshot, expected, actual); // Report the failure to the viewer
            throw e; // Rethrow for junit to handle
        }
    }

}
