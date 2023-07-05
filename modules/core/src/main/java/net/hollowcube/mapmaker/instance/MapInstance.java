package net.hollowcube.mapmaker.instance;

import net.hollowcube.mapmaker.event.PlayerInstanceLeaveEvent;
import net.hollowcube.mapmaker.instance.dimension.DimensionTypes;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWorld;
import net.hollowcube.polar.PolarWriter;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class MapInstance extends InstanceContainer {
    private static final InstanceManager INSTANCE_MANAGER = MinecraftServer.getInstanceManager();


    /*

    NOTES ABOUT OBJECTS
    - Kinda like block handlers in that there will be a registry of object types
    - Each object will have an ID, position (xyz as doubles), a bounding box, and some type specific data
    - They can be managed/manage a variety of things:
      - Blocks in the world / handlers
      - Entities in the world
      - Regions (which do not have an associated world entity)

    - They will be indexed by the following:
      - ID
      - Position (returns first matching)
      - Type
      - Bounding box (will have intersection probably)
    - They exist per chunk, although neighbors


    - Some examples
      - Pressure plate triggers (block handler in world + settings in object)
      - Decorative entities (like paintings, item frames, etc)
      - Markers







     */




    public MapInstance(@NotNull String dimensionName) {
        this(dimensionName, DimensionTypes.FULL_BRIGHT);
    }

    public MapInstance(@NotNull String dimensionName, @NotNull DimensionType dimensionType) {
        super(UUID.randomUUID(), dimensionType, null, NamespaceID.from(dimensionName));

        setTimeRate(0); //todo eventually this should be a map setting

        // Lighting and dummy chunk loader. The chunk loader will be replaced if there is world data
        // for the map to load, otherwise we keep this one.
//        setChunkSupplier(LightingChunk::new);
        setChunkLoader(new PolarLoader(new PolarWorld(
                PolarWorld.LATEST_VERSION,
                PolarWorld.CompressionType.ZSTD,
                (byte) -4, (byte) 19,
                new ArrayList<>()
        )));

        eventNode().addListener(RemoveEntityFromInstanceEvent.class, this::handleEntityRemoved);

        INSTANCE_MANAGER.registerInstance(this);
    }

    public void load(byte @NotNull [] worldData) {
        try {
            var loader = new PolarLoader(new ByteArrayInputStream(worldData));
            setChunkLoader(loader);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Blocking
    public byte @NotNull [] save() {
        saveChunksToStorage().join();
        var polarWorld = ((PolarLoader) getChunkLoader()).world();
        return PolarWriter.write(polarWorld);
    }

    public void unload() {
        INSTANCE_MANAGER.unregisterInstance(this);
    }

    private void handleEntityRemoved(@NotNull RemoveEntityFromInstanceEvent event) {
        if (event.getEntity() instanceof Player player) {
            EventDispatcher.call(new PlayerInstanceLeaveEvent(player, event.getInstance()));
        }
    }
}
