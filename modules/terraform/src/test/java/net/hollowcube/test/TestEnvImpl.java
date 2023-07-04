package net.hollowcube.test;

import net.hollowcube.test.internal.TestInstance;
import net.minestom.server.ServerProcess;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class TestEnvImpl implements TestEnv {
    //todo make this less hacky
    private static final Path ROOT_PATH = Path.of(".");
    public static final Path TEMP_PATH = ROOT_PATH.resolve("build/tmp/test");
    public static final Path RESOURCES_PATH = ROOT_PATH.resolve("src/test/resources");

    public static final boolean WRITE_SNAPSHOTS = System.getenv("WRITE_SNAPSHOTS") != null;

    private final String uniqueTestId;
    private final String testId;
    private final ServerProcess process;

    private int instanceCounter = 0;

    public TestEnvImpl(@NotNull String uniqueTestId, @NotNull String testId, @NotNull ServerProcess process) {
        this.uniqueTestId = uniqueTestId;
        this.testId = testId;
        this.process = process;
    }

    public @NotNull String getUniqueTestId() {
        return testId + "-" + uniqueTestId.hashCode();
    }

    @Override
    public @NotNull ServerProcess process() {
        return process;
    }

    @Override
    public @NotNull Instance createEmptyInstance()  {
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

}
