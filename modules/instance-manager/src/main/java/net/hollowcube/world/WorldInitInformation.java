package net.hollowcube.world;

import net.hollowcube.world.creation.chunkgenerator.ChunkGeneratorType;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;

public class WorldInitInformation {
    private static final Pos DEFAULT_SPAWN_POSITION = new Pos(0.5, 60, 0.5);
    private final Instance instance;
    private final String id;
    private final String seaweedFileId; // are we using seaweed? if not then it can be renamed to something generic
    private final String alias;
    private final int minecraftVersion;
    private final ChunkGeneratorType chunkGeneratorType;
    //private final double worldBorderSize; add to extended instances in map maker, etc
    private final Pos spawnPosition;

    /**
     * Constructor for minimal required world init information.
     */
    protected WorldInitInformation(Instance instance, String id, int minecraftVersion, ChunkGeneratorType chunkGeneratorType) {
        this.instance = instance;
        this.id = id;
        this.seaweedFileId = null;
        this.alias = null;
        this.minecraftVersion = minecraftVersion;
        this.chunkGeneratorType = chunkGeneratorType;
        this.spawnPosition = DEFAULT_SPAWN_POSITION;
    }

    /**
     * Constructor with all world init information.
     */
    protected WorldInitInformation(
            Instance instance,
            String id,
            String seaweedFileId,
            String alias,
            int minecraftVersion,
            ChunkGeneratorType chunkGeneratorType,
            Pos spawnPosition) {
        this.instance = instance;
        this.id = id;
        this.seaweedFileId = seaweedFileId;
        this.alias = alias;
        this.minecraftVersion = minecraftVersion;
        this.chunkGeneratorType = chunkGeneratorType;
        this.spawnPosition = spawnPosition;
    }

    public Instance getInstance() {
        return instance;
    }

    public String getId() {
        return id;
    }

    public String getSeaweedFileId() {
        return this.seaweedFileId;
    }

    public String getAlias() {
        return this.alias;
    }

    public int getMinecraftVersion() {
        return this.minecraftVersion;
    }

    public ChunkGeneratorType getChunkGeneratorType() {
        return this.chunkGeneratorType;
    }

    public Pos getSpawnPosition() {
        return this.spawnPosition;
    }
}
