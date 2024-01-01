package net.hollowcube.map.world.polar;

import net.hollowcube.map.entity.MapEntity;
import net.hollowcube.map.world.MapWorld;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
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

        mapWorld.biomes().write(buffer);
    }

    @Override
    public void saveChunkData(@NotNull Chunk chunk, @NotNull NetworkBuffer buffer) {
        buffer.write(NetworkBuffer.VAR_INT, VERSION_LATEST);

        var entities = getEntities(chunk);
        buffer.write(NetworkBuffer.VAR_INT, entities.size());
        for (var entity : entities) {

            buffer.write(NetworkBuffer.STRING, entity.getEntityType().name());
            buffer.write(NetworkBuffer.UUID, entity.getUuid());
            buffer.write(NetworkBuffer.VECTOR3D, entity.getPosition());
            buffer.write(NetworkBuffer.FLOAT, entity.getPosition().yaw());
            buffer.write(NetworkBuffer.FLOAT, entity.getPosition().pitch());

            entity.save(buffer);
        }
    }

    private @NotNull Set<MapEntity> getEntities(@NotNull Chunk chunk) {
        var entities = chunk.getInstance().getChunkEntities(chunk);
        return entities.stream()
                .filter(e -> e instanceof MapEntity)
                .map(e -> (MapEntity) e)
                .collect(Collectors.toSet());
    }
}
