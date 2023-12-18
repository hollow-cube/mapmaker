package net.hollowcube.map.world.polar;

import net.hollowcube.map.world.MapWorld;
import net.hollowcube.polar.PolarWorldAccess;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("UnstableApiUsage")
public class ReadWorldAccess implements PolarWorldAccess {
    private final Logger logger = LoggerFactory.getLogger(ReadWorldAccess.class);

    public static final int VERSION = 1; // Versioning changes to world data

    protected final MapWorld mapWorld;

    public ReadWorldAccess(@NotNull MapWorld mapWorld) {
        this.mapWorld = mapWorld;
    }

    @Override
    public void loadWorldData(@NotNull Instance instance, @Nullable NetworkBuffer buffer) {
        if (buffer == null) return;

        int version;
        try {
            version = buffer.read(NetworkBuffer.BYTE);
        } catch (IndexOutOfBoundsException ignored) {
            // Legacy support from before there was any user data at all.
            return;
        }
        logger.debug("reading polar world data (version {})", version);

        mapWorld.biomes().read(buffer);
    }

    @Override
    public @NotNull Biome getBiome(@NotNull String name) {
        return Biome.PLAINS;
    }

    @Override
    public @NotNull String getBiomeName(int id) {
        return "plains";
    }
}
