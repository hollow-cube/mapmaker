package net.hollowcube.mapmaker.util;

import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.IChunkLoader;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class NoopChunkLoader implements IChunkLoader {

    public static final NoopChunkLoader INSTANCE = new NoopChunkLoader();

    private NoopChunkLoader() {

    }

    @Override
    public @Nullable Chunk loadChunk(@NotNull Instance instance, int chunkX, int chunkZ) {
        return null;
    }

    @Override
    public void saveChunk(@NotNull Chunk chunk) {

    }
}
