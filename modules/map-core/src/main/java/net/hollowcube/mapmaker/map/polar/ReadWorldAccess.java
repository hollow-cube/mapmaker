package net.hollowcube.mapmaker.map.polar;

import net.hollowcube.datafix.DataFixer;
import net.hollowcube.mapmaker.map.AbstractMapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.entity.MapEntityType;
import net.hollowcube.mapmaker.map.instance.ChunkExt;
import net.hollowcube.mapmaker.map.instance.Heightmaps;
import net.hollowcube.mapmaker.map.util.NbtUtil;
import net.hollowcube.mapmaker.map.util.datafix.HCDataTypes;
import net.hollowcube.polar.PolarWorldAccess;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.IntArrayBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Entity;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.tag.TagHandler;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.utils.UUIDUtils;
import net.minestom.server.utils.validate.Check;
import net.minestom.server.world.biome.Biome;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class ReadWorldAccess implements PolarWorldAccess {
    private final Logger logger = LoggerFactory.getLogger(ReadWorldAccess.class);

    public static final int VERSION_LATEST = 5; // Versioning changes to world data
    public static final int VERSION_PRE_WORLD_NBT = 3;
    public static final int VERSION_PRE_CHUNK_NBT = 4;

    protected final AbstractMapWorld<?, ?> mapWorld;

    private int dataVersion = -1;

    public ReadWorldAccess(AbstractMapWorld<?, ?> mapWorld) {
        this.mapWorld = mapWorld;
    }

    @Override
    public void loadWorldData(Instance instance, @Nullable NetworkBuffer buffer) {
        mapWorld.loadWorldTag(loadWorldTag(buffer));
    }

    private TagReadable loadWorldTag(@Nullable NetworkBuffer buffer) {
        if (buffer == null) return TagHandler.newHandler();

        int version;
        try {
            version = buffer.read(NetworkBuffer.BYTE);
        } catch (IndexOutOfBoundsException ignored) {
            // Legacy support from before there was any user data at all.
            return TagHandler.newHandler();
        }
        logger.debug("reading polar world data (user version {})", version);

        if (version <= VERSION_PRE_WORLD_NBT) {
            // Legacy support
            return TagHandler.newHandler();
        }

        if (version > VERSION_PRE_CHUNK_NBT) {
            dataVersion = buffer.read(NetworkBuffer.VAR_INT);
        }

        if (buffer.read(NetworkBuffer.NBT) instanceof CompoundBinaryTag worldData) {
            if (dataVersion != -1 && dataVersion < DataFixer.maxVersion()) {
                worldData = (CompoundBinaryTag) DataFixer.upgrade(HCDataTypes.WORLD, Transcoder.NBT,
                        worldData, dataVersion, DataFixer.maxVersion());
            }

            return TagHandler.fromCompound(worldData);
        }

        return TagHandler.newHandler();
    }

    @Override
    public void loadChunkData(Chunk chunk, @Nullable NetworkBuffer buffer) {
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
        if (dataVersion < DataFixer.maxVersion()) {
            chunkData = (CompoundBinaryTag) DataFixer.upgrade(HCDataTypes.CHUNK, Transcoder.NBT,
                    chunkData, dataVersion, DataFixer.maxVersion());
        }

        // Load the chunk NBT
        ListBinaryTag entities = chunkData.getList("entities", BinaryTagTypes.COMPOUND);
        for (var entity : entities) loadEntityWithPassengers(chunk, (CompoundBinaryTag) entity);
    }

    protected static final int WORLD_BOTTOM = 16;

    @Override
    public void loadHeightmaps(Chunk rawChunk, int[][] heightmaps) {
        if (!(rawChunk instanceof ChunkExt chunk)) return;

        chunk.loadHeightmap(Heightmaps.WORLD_SURFACE, heightmaps[Heightmaps.WORLD_SURFACE]);
        chunk.loadHeightmap(Heightmaps.MOTION_BLOCKING, heightmaps[Heightmaps.MOTION_BLOCKING]);
        chunk.loadHeightmap(Heightmaps.WORLD_BOTTOM, heightmaps[WORLD_BOTTOM]);
    }

    @Override
    public int getBiomeId(String name) {
        var key = RegistryKey.<Biome>unsafeOf(name);
        var id = mapWorld.biomes().getId(key);
        return id == -1 ? 0 : id;
    }

    @Override
    public String getBiomeName(int id) {
        return Objects.requireNonNullElse(mapWorld.biomes().getKey(id), Biome.PLAINS).name();
    }

    private void loadEntityWithPassengers(Chunk chunk, CompoundBinaryTag tag) {
        var entity = createEntityUnspawned(tag);
        if (entity == null) return;

        var pos = NbtUtil.from(tag.getList("Pos", BinaryTagTypes.DOUBLE));
        var spawnPosition = NbtUtil.readRotation(pos, tag.getList("Rotation", BinaryTagTypes.FLOAT));

        var passengers = new ArrayList<Entity>();
        for (var passengerTag : tag.getList("Passengers", BinaryTagTypes.COMPOUND)) {
            var passenger = createEntityUnspawned((CompoundBinaryTag) passengerTag);
            if (passenger == null) continue;
            passengers.add(passenger);
        }

        // Spawn in the world with passengers
        entity.setInstance(chunk.getInstance(), spawnPosition);
        for (var passenger : passengers)
            passenger.setInstance(chunk.getInstance(), spawnPosition);

        // Add passengers on first tick because instance is not fully set up yet.
        entity.scheduleNextTick(ignored -> passengers.forEach(entity::addPassenger));
    }

    @SuppressWarnings("UnstableApiUsage")
    private @Nullable Entity createEntityUnspawned(CompoundBinaryTag tag) {
        EntityType entityType = EntityType.fromKey(tag.getString("id"));
        if (entityType == null) {
            logger.warn("Unknown entity type {}", tag.getString("id"));
            return null;
        }
        UUID uuid = tag.get("UUID") instanceof IntArrayBinaryTag uuidTag ? UUIDUtils.fromNbt(uuidTag) : UUID.randomUUID();

        var entity = MapEntityType.create(entityType, uuid);
        if (entity instanceof MapEntity<?> mapEntity) mapEntity.readData(tag);

        return entity;
    }

    @Deprecated // WARN :: No new calls should be added to this function
    private CompletableFuture<Void> legacyReadEntity(Chunk chunk, NetworkBuffer buffer) {
        var entityType = buffer.read(NetworkBuffer.STRING);
        var uuid = buffer.read(NetworkBuffer.UUID);
        var pos = buffer.read(NetworkBuffer.VECTOR3D);
        var yaw = buffer.read(NetworkBuffer.FLOAT);
        var pitch = buffer.read(NetworkBuffer.FLOAT);

        var entity = MapEntityType.create(entityType, uuid);
        if (entity instanceof MapEntity<?> mapEntity)
            mapEntity.legacyLoad(buffer, VERSION_LATEST);

        return entity.setInstance(chunk.getInstance(), new Pos(pos, yaw, pitch));
    }
}
