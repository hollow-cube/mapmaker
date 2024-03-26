package net.hollowcube.mapmaker.map.polar;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.marker.MarkerLoader;
import net.hollowcube.mapmaker.map.instance.ChunkExt;
import net.hollowcube.mapmaker.map.instance.Heightmaps;
import net.hollowcube.polar.PolarWorldAccess;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jglrxavpok.hephaistos.nbt.NBTCompound;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class ReadWorldAccess implements PolarWorldAccess {
    private final Logger logger = LoggerFactory.getLogger(ReadWorldAccess.class);

    public static final int VERSION_LATEST = 4; // Versioning changes to world data
    public static final int VERSION_PRE_NBT = 3;

    protected final MapWorld mapWorld;
    private final MarkerLoader markerLoader;

    public ReadWorldAccess(@NotNull MapWorld mapWorld) {
        this(mapWorld, null);
    }

    public ReadWorldAccess(@NotNull MapWorld mapWorld, @Nullable MarkerLoader markerLoader) {
        this.mapWorld = mapWorld;
        this.markerLoader = markerLoader;
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

        if (version <= VERSION_PRE_NBT) {
            // Legacy support
            return;
        }

        // There is an issue here. I cannot call TagHandler#updateContent because it will wipe any existing tags.
        // I cannot get the compound, merge, and then #updateContent because it will still wipe any transient tags.
        // The result solution is to set each individual key on its own, which is really yikes. But it does work...
        var worldTag = mapWorld.instance().tagHandler();
        if (buffer.read(NetworkBuffer.NBT) instanceof NBTCompound worldData) {
            for (var entry : worldData.getEntries()) {
                worldTag.setTag(Tag.NBT(entry.getKey()), entry.getValue());
            }
        }
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

    protected static final int WORLD_BOTTOM = 16;

    @Override
    public void loadHeightmaps(@NotNull Chunk rawChunk, int[][] heightmaps) {
        if (!(rawChunk instanceof ChunkExt chunk)) return;

        chunk.loadHeightmap(Heightmaps.WORLD_SURFACE, heightmaps[Heightmaps.WORLD_SURFACE]);
        chunk.loadHeightmap(Heightmaps.MOTION_BLOCKING, heightmaps[Heightmaps.MOTION_BLOCKING]);
        chunk.loadHeightmap(Heightmaps.WORLD_BOTTOM, heightmaps[WORLD_BOTTOM]);

    }

    @Override
    public @NotNull Biome getBiome(@NotNull String name) {
        return mapWorld.biomes().getLoadedBiome(name);
    }

    @Override
    public @NotNull String getBiomeName(int id) {
        return mapWorld.biomes().getLoadedBiomeName(id);
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

        var spawnPosition = new Pos(pos, yaw, pitch);
        if (entity instanceof MarkerEntity marker && markerLoader != null) {
            boolean shouldSpawn = markerLoader.loadMarker(mapWorld, marker.getType(), marker.getMarkerData(), spawnPosition);
            if (!shouldSpawn) {
                entity.remove();
                return CompletableFuture.completedFuture(null);
            }
        }

        return entity.setInstance(chunk.getInstance(), new Pos(pos, yaw, pitch));
    }
}
