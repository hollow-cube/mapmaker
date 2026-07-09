package net.hollowcube.mapmaker.map.instance;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.event.PlayerInstanceLeaveEvent;
import net.hollowcube.mapmaker.instance.dimension.DimensionTypes;
import net.hollowcube.mapmaker.map.ReadableMapData;
import net.hollowcube.mapmaker.map.polar.PolarDataFixer;
import net.hollowcube.polar.*;
import net.kyori.adventure.key.Key;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.ChunkLoader;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.utils.chunk.ChunkSupplier;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class MapInstance extends InstanceContainer {

    public enum LightingMode {
        // Run the light engine on changes.
        GENERATED,
        // Load lighting data from the world, but dont run the light engine.
        LOADED,
        // Don't load light, use static light data.
        FULL_BRIGHT,
    }

    private final LightingMode lightingMode;
    private final ChunkSupplier chunkSupplier;

    public MapInstance(@NotNull String dimensionName, @NotNull LightingMode lightingMode) {
        this(dimensionName, lightingMode == LightingMode.FULL_BRIGHT ? DimensionTypes.FULL_BRIGHT : DimensionType.OVERWORLD, lightingMode);
    }

    public MapInstance(@NotNull String dimensionName, @NotNull RegistryKey<DimensionType> dimensionType, @NotNull LightingMode lightingMode) {
        super(UUID.randomUUID(), dimensionType, null, Key.key(dimensionName));
        this.lightingMode = lightingMode;

        defaultClock().pause();
        setTime(6000);

        // Lighting and dummy chunk loader. The chunk loader will be replaced if there is world data
        // for the map to load, otherwise we keep this one.
        if (lightingMode == LightingMode.GENERATED) {
            this.chunkSupplier = LitChunk::new;
        } else if (lightingMode == LightingMode.LOADED) {
            this.chunkSupplier = UnlitChunk::new;
        } else {
            var fullBrightLightData = UnlitChunk.createStaticLightData(this, 15, 0);
            this.chunkSupplier = (instance, chunkX, chunkZ) -> new UnlitChunk(instance, chunkX, chunkZ, fullBrightLightData);
        }
        setChunkSupplier(chunkSupplier);
        setChunkLoader(new PolarLoader(new PolarWorld()));

        eventNode().addListener(RemoveEntityFromInstanceEvent.class, this::handleEntityRemoved);

        MinecraftServer.getInstanceManager().registerInstance(this);
    }

    public void load(byte @NotNull [] worldData, @Nullable PolarWorldAccess worldAccess) {
        load(PolarReader.read(worldData, PolarDataFixer.INSTANCE), worldAccess);
    }

    public void load(@NotNull PolarWorld world, @Nullable PolarWorldAccess worldAccess) {
        var loader = new PolarLoader(world);
        if (worldAccess != null) loader.setWorldAccess(worldAccess);
        if (lightingMode == LightingMode.FULL_BRIGHT) loader.setLoadLighting(false);
        setChunkLoader(loader);

        // Load the world data
        loader.loadInstance(this);

        // Load all the chunks immediately
        var loadingChunks = new ArrayList<CompletableFuture<Chunk>>();
        loader.world().chunks().forEach(chunk -> loadingChunks.add(loadChunk(chunk.x(), chunk.z())));
        FutureUtil.getUnchecked(CompletableFuture.allOf(loadingChunks.toArray(CompletableFuture[]::new)));

        // Delete the polar world to avoid the second copy of the world data
        setChunkLoader(ChunkLoader.noop());
    }

    public void loadStream(@NotNull ReadableMapData data, @Nullable PolarWorldAccess worldAccess) {
        FutureUtil.getUnchecked(PolarLoader.streamLoad(this, data.data(), data.length(),
                PolarDataFixer.INSTANCE, worldAccess, lightingMode != LightingMode.FULL_BRIGHT));
        // Chunks inside the border must be editable even without world data (eg unloaded chunks),
        // so EmptyChunk is only used outside of it.
        setChunkSupplier((instance, chunkX, chunkZ) -> chunkIntersectsBorder(chunkX, chunkZ)
            ? chunkSupplier.createChunk(instance, chunkX, chunkZ)
                : new EmptyChunk(instance, chunkX, chunkZ));
        setChunkLoader(ChunkLoader.noop());
    }

    private boolean chunkIntersectsBorder(int chunkX, int chunkZ) {
        var border = getWorldBorder();
        final double radius = border.diameter() / 2d;
        final int minX = chunkX * Chunk.CHUNK_SIZE_X, minZ = chunkZ * Chunk.CHUNK_SIZE_Z;
        return minX < border.centerX() + radius && minX + Chunk.CHUNK_SIZE_X - 1 >= border.centerX() - radius
                && minZ < border.centerZ() + radius && minZ + Chunk.CHUNK_SIZE_Z - 1 >= border.centerZ() - radius;
    }

    @Blocking
    public byte @NotNull [] save(@NotNull PolarWorldAccess worldAccess) {
        // Save through a local loader without installing it on the instance, so a chunk load
        // racing the save can never read from (or write into) the partially written world.
        var polarWorld = new PolarWorld(getCachedDimensionType());
        var loader = new PolarLoader(polarWorld)
                .setWorldAccess(worldAccess);

        polarWorld.userData(NetworkBuffer.makeArray(buffer -> worldAccess.saveWorldData(this, buffer)));
        loader.saveChunks(getChunks().stream()
                .filter(chunk -> !(chunk instanceof EmptyChunk))
                .toList());

        if (polarWorld.chunks().isEmpty())
            throw new IllegalStateException("Avoiding saving empty instance!");
        return PolarWriter.write(polarWorld, PolarDataFixer.INSTANCE);
    }

    public void unload() {
        if (!getPlayers().isEmpty()) {
            // Something went wrong removing people, but to not unregister would be worse so kick the players.
            Set.copyOf(getPlayers()).forEach(player -> player.kick("Map unloaded"));
        }
        MinecraftServer.getInstanceManager().unregisterInstance(this);
    }

    private void handleEntityRemoved(@NotNull RemoveEntityFromInstanceEvent event) {
        if (event.getEntity() instanceof Player player) {
            EventDispatcher.call(new PlayerInstanceLeaveEvent(player, event.getInstance()));
        }
    }
}
