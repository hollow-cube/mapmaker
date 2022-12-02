package net.hollowcube.world;

import net.hollowcube.api.IWorld;
import net.hollowcube.world.creation.TemporaryWorld;
import net.hollowcube.world.creation.chunkgenerator.ChunkGeneratorType;
import net.hollowcube.world.unload.WorldSaveResult;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;

import java.util.function.Consumer;

public class World extends TemporaryWorld implements IWorld {
    private final String id;
    private final String seaweedFileId;
    private String alias;

    public World(WorldInitInformation worldInitInformation) {
        super(
                worldInitInformation.getInstance(),
                worldInitInformation.getMinecraftVersion(),
                worldInitInformation.getChunkGeneratorType(),
                worldInitInformation.getSpawnPosition());
        this.id = worldInitInformation.getId();
        this.seaweedFileId = worldInitInformation.getSeaweedFileId();
        this.alias = worldInitInformation.getAlias();
    }

    public World(
            Instance instance,
            String id,
            String seaweedFileId,
            String alias,
            int minecraftVersion,
            ChunkGeneratorType chunkGeneratorType,
            Pos spawnPosition)
    {
        super(instance, minecraftVersion, chunkGeneratorType, spawnPosition);
        this.id = id;
        this.seaweedFileId = seaweedFileId;
        this.alias = alias;
    }

    @Override
    public void saveWorld() {
        this.saveWorld(null);
    }

    @Override
    public void saveWorld(Consumer<WorldSaveResult> consumer) {
        //TODO implement save world
    }

    @Override
    public void saveAndUnloadWorld() {
        this.saveAndUnloadWorld(null);
    }

    @Override
    public void saveAndUnloadWorld(Consumer<WorldSaveResult> consumer) {
        // bro i hate java i'm still trying to understand why this is even parsable in this language
        this.saveWorld(worldSaveResult -> World.this.unloadWorld(() -> {
            if (consumer != null) {
                consumer.accept(worldSaveResult);
            }
        }));
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public String getSeaweedFileId() {
        return this.seaweedFileId;
    }

    @Override
    public String getAlias() {
        if (this.alias == null) {
            return this.id;
        }
        return this.alias;
    }

    @Override
    public void setAlias(String alias) {
        this.alias = alias;
    }
}
