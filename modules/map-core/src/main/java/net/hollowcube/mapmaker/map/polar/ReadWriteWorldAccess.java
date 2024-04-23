package net.hollowcube.mapmaker.map.polar;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.entity.MapEntity;
import net.hollowcube.mapmaker.map.instance.ChunkExt;
import net.hollowcube.mapmaker.map.instance.Heightmaps;
import net.hollowcube.mapmaker.map.util.NbtUtil;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.UniqueIdUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public class ReadWriteWorldAccess extends ReadWorldAccess {
    private static final Logger logger = LoggerFactory.getLogger(ReadWriteWorldAccess.class);

    public ReadWriteWorldAccess(@NotNull MapWorld mapWorld) {
        super(mapWorld);
    }

    @Override
    public void saveWorldData(@NotNull Instance instance, @NotNull NetworkBuffer buffer) {
        logger.debug("writing polar world data");
        buffer.write(NetworkBuffer.BYTE, (byte) VERSION_LATEST);

        // Always write latest data version for now
        buffer.write(NetworkBuffer.VAR_INT, MapWorld.DATA_VERSION);

        var worldData = mapWorld.instance().tagHandler().asCompound();
        buffer.write(NetworkBuffer.NBT, worldData);
    }

    @Override
    public void saveChunkData(@NotNull Chunk chunk, @NotNull NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.VAR_INT, VERSION_LATEST);

        CompoundBinaryTag.Builder tag = CompoundBinaryTag.builder();
        tag.put("entities", saveEntities(chunk));

        buffer.write(NetworkBuffer.NBT, tag.build());
    }

    @Override
    public void saveHeightmaps(@NotNull Chunk rawChunk, int[][] heightmaps) {
        if (!(rawChunk instanceof ChunkExt chunk)) return;

        heightmaps[Heightmaps.WORLD_SURFACE] = chunk.saveHeightmap(Heightmaps.WORLD_SURFACE);
        heightmaps[Heightmaps.MOTION_BLOCKING] = chunk.saveHeightmap(Heightmaps.MOTION_BLOCKING);
        heightmaps[WORLD_BOTTOM] = chunk.saveHeightmap(Heightmaps.WORLD_BOTTOM);
    }

    private @NotNull ListBinaryTag saveEntities(@NotNull Chunk chunk) {
        ListBinaryTag.Builder<CompoundBinaryTag> entitiesTag = ListBinaryTag.builder(BinaryTagTypes.COMPOUND);

        for (var entity : getEntities(chunk)) {
            CompoundBinaryTag.Builder tag = CompoundBinaryTag.builder();

            tag.putString("id", entity.getEntityType().name());
            tag.put("uuid", UniqueIdUtils.toNbt(entity.getUuid()));
            tag.put("Pos", NbtUtil.into(entity.getPosition()));
            tag.put("Rotation", NbtUtil.writeRotation(entity.getPosition()));
            entity.writeData(tag);

            entitiesTag.add(tag.build());
        }

        return entitiesTag.build();
    }

    private @NotNull Set<MapEntity> getEntities(@NotNull Chunk chunk) {
        var entities = chunk.getInstance().getChunkEntities(chunk);
        return entities.stream()
                .filter(e -> e instanceof MapEntity)
                .map(e -> (MapEntity) e)
                .collect(Collectors.toSet());
    }
}
