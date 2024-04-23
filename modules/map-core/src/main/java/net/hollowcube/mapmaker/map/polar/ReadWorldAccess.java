package net.hollowcube.mapmaker.map.polar;

import ca.spottedleaf.dataconverter.minecraft.MCDataConverter;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.marker.MarkerLoader;
import net.hollowcube.mapmaker.map.instance.ChunkExt;
import net.hollowcube.mapmaker.map.instance.Heightmaps;
import net.hollowcube.mapmaker.map.util.NbtUtil;
import net.hollowcube.mapmaker.map.util.datafix.HCTypeRegistry;
import net.hollowcube.polar.PolarWorldAccess;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.UniqueIdUtils;
import net.minestom.server.utils.validate.Check;
import net.minestom.server.world.biomes.Biome;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class ReadWorldAccess implements PolarWorldAccess {
    private final Logger logger = LoggerFactory.getLogger(ReadWorldAccess.class);

    public static final int VERSION_LATEST = 5; // Versioning changes to world data
    public static final int VERSION_PRE_WORLD_NBT = 3;
    public static final int VERSION_PRE_CHUNK_NBT = 4;

    protected final MapWorld mapWorld;
    private final MarkerLoader markerLoader;

    private int dataVersion = -1;

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

        if (version <= VERSION_PRE_WORLD_NBT) {
            // Legacy support
            return;
        }

        if (version > VERSION_PRE_CHUNK_NBT) {
            dataVersion = buffer.read(NetworkBuffer.VAR_INT);
        }

        // There is an issue here. I cannot call TagHandler#updateContent because it will wipe any existing tags.
        // I cannot get the compound, merge, and then #updateContent because it will still wipe any transient tags.
        // The result solution is to set each individual key on its own, which is really yikes. But it does work...
        var worldTag = mapWorld.instance().tagHandler();
        if (buffer.read(NetworkBuffer.NBT) instanceof CompoundBinaryTag worldData) {

            // Upgrade the world if needed
            if (dataVersion != -1 && dataVersion < MapWorld.DATA_VERSION) {
                worldData = MCDataConverter.convertTag(HCTypeRegistry.WORLD, worldData,
                        dataVersion, MapWorld.DATA_VERSION);
            }

            // Apply the tags to the world.
            for (var entry : worldData) {
                worldTag.setTag(Tag.NBT(entry.getKey()), entry.getValue());
            }
        }
    }

    @Override
    public void loadChunkData(@NotNull Chunk chunk, @Nullable NetworkBuffer buffer) {
        int version;
        try {
            version = buffer.read(NetworkBuffer.VAR_INT);
        } catch (IndexOutOfBoundsException ignored) {
            // Legacy support from before there was any user data at all.
            return;
        }

        if (version <= VERSION_PRE_CHUNK_NBT) {
            int entityCount = buffer.read(NetworkBuffer.VAR_INT);
            for (int i = 0; i < entityCount; i++) {
                legacyReadEntity(chunk, buffer); // Dont wait on this it doesnt matter.
            }
            return;
        }

        // Sanity check load order
        Check.stateCondition(dataVersion == -1, "Chunk data loaded before world data");
        var chunkData = (CompoundBinaryTag) buffer.read(NetworkBuffer.NBT);

        // Upgrade the chunk if needed
        if (dataVersion < MapWorld.DATA_VERSION) {
            chunkData = MCDataConverter.convertTag(HCTypeRegistry.CHUNK, chunkData,
                    dataVersion, MapWorld.DATA_VERSION);
        }

        // Load the chunk NBT
        ListBinaryTag entities = chunkData.getList("entities", BinaryTagTypes.COMPOUND);
        for (var entity : entities) loadEntity(chunk, (CompoundBinaryTag) entity);
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

    private void loadEntity(@NotNull Chunk chunk, @NotNull CompoundBinaryTag tag) {
        EntityType entityType = EntityType.fromNamespaceId(tag.getString("id"));
        if (entityType == null) {
            logger.warn("Unknown entity type {}", tag.getString("id"));
            return;
        }
        UUID uuid = tag.get("UUID") instanceof IntArrayBinaryTag uuidTag ? UniqueIdUtils.fromNbt(uuidTag) : UUID.randomUUID();

        var entity = MapEntityType.create(entityType, uuid);
        if (entity instanceof MapEntity mapEntity) mapEntity.readData(tag);

        var pos = NbtUtil.from(tag.getList("Pos", BinaryTagTypes.DOUBLE));
        var spawnPosition = NbtUtil.readRotation(pos, tag.getList("Rotation", BinaryTagTypes.FLOAT));

        if (tryLoadMarker(entity, spawnPosition)) return;
        entity.setInstance(chunk.getInstance(), spawnPosition);
    }

    private boolean tryLoadMarker(@NotNull Entity entity, @NotNull Pos spawnPosition) {
        if (entity instanceof MarkerEntity marker && markerLoader != null) {
            boolean shouldSpawn = markerLoader.loadMarker(mapWorld, marker.getType(), marker.getMarkerData(), spawnPosition);
            if (!shouldSpawn) {
                entity.remove();
                return true;
            }
        }
        return false;
    }

    @Deprecated // WARN :: No new calls should be added to this function
    private @NotNull CompletableFuture<Void> legacyReadEntity(@NotNull Chunk chunk, @NotNull NetworkBuffer buffer) {
        var entityType = buffer.read(NetworkBuffer.STRING);
        var uuid = buffer.read(NetworkBuffer.UUID);
        var pos = buffer.read(NetworkBuffer.VECTOR3D);
        var yaw = buffer.read(NetworkBuffer.FLOAT);
        var pitch = buffer.read(NetworkBuffer.FLOAT);

        var entity = MapEntityType.create(entityType, uuid);
        if (entity instanceof MapEntity mapEntity)
            mapEntity.legacyLoad(buffer, VERSION_LATEST);

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
