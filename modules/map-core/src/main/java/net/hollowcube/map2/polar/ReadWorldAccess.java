package net.hollowcube.map2.polar;

import net.hollowcube.map2.MapWorld;
import net.hollowcube.map2.entity.MapEntity;
import net.hollowcube.map2.entity.MapEntityType;
import net.hollowcube.polar.PolarWorldAccess;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class ReadWorldAccess implements PolarWorldAccess {
    private final Logger logger = LoggerFactory.getLogger(ReadWorldAccess.class);

    public static final int VERSION_LATEST = 3; // Versioning changes to world data
    public static final int VERSION_PRE_PROTO = 1;

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

        if (version <= VERSION_PRE_PROTO) {
            // Legacy support
            return;
        }

        mapWorld.biomes().read(buffer);
    }

    @Override
    public void loadChunkData(@NotNull Chunk chunk, @Nullable NetworkBuffer buffer) {
        int version;
        try {
            version = buffer.read(NetworkBuffer.BYTE);
        } catch (IndexOutOfBoundsException ignored) {
            // Legacy support from before there was any user data at all.
            return;
        }

        int entityCount = buffer.read(NetworkBuffer.VAR_INT);
        for (int i = 0; i < entityCount; i++) {
            readEntity(chunk, buffer); // Dont wait on this it doesnt matter.
        }
    }

    @Override
    public @NotNull Biome getBiome(@NotNull String name) {
        return mapWorld.biomes().getBiome(name);
    }

    @Override
    public @NotNull String getBiomeName(int id) {
        return mapWorld.biomes().getBiomeName(id);
    }

    private @NotNull CompletableFuture<Void> readEntity(@NotNull Chunk chunk, @NotNull NetworkBuffer buffer) {
        var entityType = buffer.read(NetworkBuffer.STRING);
        var uuid = buffer.read(NetworkBuffer.UUID);
        var pos = buffer.read(NetworkBuffer.VECTOR3D);
        var yaw = buffer.read(NetworkBuffer.FLOAT);
        var pitch = buffer.read(NetworkBuffer.FLOAT);

        var entity = MapEntityType.create(entityType, uuid);
        if (entity instanceof MapEntity mapEntity)
            mapEntity.load(buffer, VERSION_LATEST);

        return entity.setInstance(chunk.getInstance(), new Pos(pos, yaw, pitch));
    }
}
