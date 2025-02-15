package net.hollowcube.mapmaker.map.polar;

import net.hollowcube.polar.PolarWorldAccess;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Delegates to another {@link PolarWorldAccess} and runs a callback when the world is loaded data is loaded.
 */
@SuppressWarnings("UnstableApiUsage")
public record LoadingWorldAccess(
        @NotNull PolarWorldAccess delegate,
        @NotNull Runnable onLoad
) implements PolarWorldAccess {

    @Override
    public void loadWorldData(@NotNull Instance instance, @Nullable NetworkBuffer userData) {
        this.delegate.loadWorldData(instance, userData);
        this.onLoad.run();
    }

    @Override
    public void saveWorldData(@NotNull Instance instance, @NotNull NetworkBuffer userData) {
        this.delegate.saveWorldData(instance, userData);
    }

    @Override
    public void loadChunkData(@NotNull Chunk chunk, @Nullable NetworkBuffer userData) {
        this.delegate.loadChunkData(chunk, userData);
    }

    @Override
    public void saveChunkData(@NotNull Chunk chunk, @NotNull NetworkBuffer userData) {
        this.delegate.saveChunkData(chunk, userData);
    }

    @Override
    public void loadHeightmaps(@NotNull Chunk chunk, int[][] heightmaps) {
        this.delegate.loadHeightmaps(chunk, heightmaps);
    }

    @Override
    public void saveHeightmaps(@NotNull Chunk chunk, int[][] heightmaps) {
        this.delegate.saveHeightmaps(chunk, heightmaps);
    }

    @Override
    public DynamicRegistry.@NotNull Key<Biome> getBiome(@NotNull String name) {
        return this.delegate.getBiome(name);
    }

    @Override
    public @NotNull String getBiomeName(int id) {
        return this.delegate.getBiomeName(id);
    }

}
