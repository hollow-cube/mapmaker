package net.hollowcube.mapmaker.map.instance;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.event.PlayerInstanceLeaveEvent;
import net.hollowcube.mapmaker.instance.dimension.DimensionTypes;
import net.hollowcube.mapmaker.map.ReadableMapData;
import net.hollowcube.mapmaker.map.polar.PolarDataFixer;
import net.hollowcube.mapmaker.util.NoopChunkLoader;
import net.hollowcube.polar.*;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.network.packet.server.play.data.LightData;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class MapInstance extends InstanceContainer {
    private static final InstanceManager INSTANCE_MANAGER = MinecraftServer.getInstanceManager();

    private final boolean hasLighting;

    public MapInstance(@NotNull String dimensionName, boolean hasLighting) {
        this(dimensionName, hasLighting ? DimensionType.OVERWORLD : DimensionTypes.FULL_BRIGHT, hasLighting);
    }

    public MapInstance(@NotNull String dimensionName, @NotNull DynamicRegistry.Key<DimensionType> dimensionType, boolean hasLighting) {
        super(UUID.randomUUID(), dimensionType, null, NamespaceID.from(dimensionName));
        this.hasLighting = hasLighting;

        setTimeRate(0); //todo eventually this should be a map setting
        setTime(6000);

        // Lighting and dummy chunk loader. The chunk loader will be replaced if there is world data
        // for the map to load, otherwise we keep this one.
        if (hasLighting) {
            setChunkSupplier(LitChunk::new);
        } else {
            BitSet fullBitSet = new BitSet();
            fullBitSet.set(0, this.getCachedDimensionType().height() / 16);
            byte[] full = new byte[2048];
            Arrays.fill(full, (byte) 0b1111_1111);

            byte[][] empty = new byte[this.getCachedDimensionType().height() / 16][];
            Arrays.fill(empty, full);

            var fullBrightLightData = new LightData(
                    fullBitSet,
                    new BitSet(),
                    new BitSet(),
                    fullBitSet,
                    Arrays.asList(empty),
                    List.of()
            );
            setChunkSupplier((instance, chunkX, chunkZ) -> new UnlitChunk(instance, chunkX, chunkZ, fullBrightLightData));
        }
        setChunkLoader(new PolarLoader(new PolarWorld()));

        eventNode().addListener(RemoveEntityFromInstanceEvent.class, this::handleEntityRemoved);

        INSTANCE_MANAGER.registerInstance(this);
    }

    public void load(byte @NotNull [] worldData, @Nullable PolarWorldAccess worldAccess) {
        load(PolarReader.read(worldData, PolarDataFixer.INSTANCE), worldAccess);
    }

    public void load(@NotNull PolarWorld world, @Nullable PolarWorldAccess worldAccess) {
        var loader = new PolarLoader(world);
        if (worldAccess != null) loader.setWorldAccess(worldAccess);
        if (!hasLighting) loader.setLoadLighting(false);
        setChunkLoader(loader);

        // Load the world data
        loader.loadInstance(this);

        // Load all the chunks immediately
        var loadingChunks = new ArrayList<CompletableFuture<Chunk>>();
        loader.world().chunks().forEach(chunk -> loadingChunks.add(loadChunk(chunk.x(), chunk.z())));
        FutureUtil.getUnchecked(CompletableFuture.allOf(loadingChunks.toArray(CompletableFuture[]::new)));

        // Delete the polar world to avoid the second copy of the world data
        setChunkLoader(NoopChunkLoader.INSTANCE);
    }

    public void loadStream(@NotNull ReadableMapData data, @Nullable PolarWorldAccess worldAccess) {
        FutureUtil.getUnchecked(PolarLoader.streamLoad(this, data.data(), data.length(),
                PolarDataFixer.INSTANCE, worldAccess, hasLighting));
    }

    @Blocking
    public byte @NotNull [] save(@Nullable PolarWorldAccess worldAccess) {
        // Since we deleted the loader we need a new one
        var loader = new PolarLoader(new PolarWorld());
        if (worldAccess != null) loader.setWorldAccess(worldAccess);
        setChunkLoader(loader);

        FutureUtil.getUnchecked(saveInstance());

        var polarWorld = loader.world();
        var worldData = PolarWriter.write(polarWorld);

        // Reset to noop
        setChunkLoader(NoopChunkLoader.INSTANCE);

        return worldData;
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
