package net.hollowcube.mapmaker.hub.world;

import net.hollowcube.common.util.ExtraTags;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.find_a_new_home.hotbar.HubHotbar;
import net.hollowcube.mapmaker.hub.world.generator.HubGenerators;
import net.hollowcube.mapmaker.instance.MapInstance;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.util.NoopChunkLoader;
import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarReader;
import net.minestom.server.MinecraftServer;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class HubWorld {
    private static final System.Logger logger = System.getLogger(HubWorld.class.getName());

    public static final Tag<Boolean> MARKER = Tag.Boolean("mapmaker:hub/marker"); //todo unnecessary
    private static final Tag<HubWorld> THIS_TAG = ExtraTags.Transient("mapmaker:hub/world");

    public static @NotNull HubWorld fromInstance(@NotNull Instance instance) {
        return Objects.requireNonNull(optionalFromInstance(instance));
    }

    public static @Nullable HubWorld optionalFromInstance(@Nullable Instance instance) {
        if (instance == null) return null;
        return instance.getTag(THIS_TAG);
    }

    private final HubServer server;

    private final MapInstance instance;

    public HubWorld(@NotNull HubServer server) {
        this.server = server;

        instance = new MapInstance("mapmaker:hub");
        instance.setTag(MARKER, true);
        instance.setTag(THIS_TAG, this);
        instance.setGenerator(HubGenerators.stoneWorld());

        var eventNode = instance.eventNode();
        eventNode.addChild(HubHotbar.eventNode());

        //todo add some WorldConfig options passed on world create. Can add some useful/common ones
        // like setting a generator (default to void probably), preventing block placement/breaking, etc.
        eventNode.addListener(PlayerBlockBreakEvent.class, this::preventBlockBreak);
        eventNode.addListener(PlayerBlockPlaceEvent.class, this::preventBlockPlace);

        //todo load the world
    }

    public @NotNull HubServer server() {
        return server;
    }

    public @NotNull Instance instance() {
        return instance;
    }

    public void loadWorld() {
        var spawnMapId = MapData.SPAWN_MAP_ID;
        if (spawnMapId != null) {
            var mapData = server().mapService().getMapWorld(spawnMapId, false);
            assert mapData != null;
            instance.setChunkLoader(new PolarLoader(PolarReader.read(mapData)));
        } else {
            try (var is = getClass().getResourceAsStream("/spawn/hcspawn.polar")) {
                if (is == null) throw new IOException("hcspawn.polar not found");
                instance.setChunkLoader(new PolarLoader(is));
            } catch (IOException e) {
                MinecraftServer.getExceptionManager().handleException(e);
            }
        }

        var loadingChunks = new ArrayList<CompletableFuture<Chunk>>();
        ChunkUtils.forChunksInRange(0, 0, 16, (x, z) -> loadingChunks.add(instance.loadChunk(x, z)));
        CompletableFuture.allOf(loadingChunks.toArray(CompletableFuture[]::new))
                .thenRun(() -> logger.log(System.Logger.Level.INFO, "Loaded spawn chunks"));

        // Since we never save this world, delete the polar world and associated copy of the world
        instance.setChunkLoader(NoopChunkLoader.INSTANCE);
    }

    private void preventBlockBreak(PlayerBlockBreakEvent event) {
        event.setCancelled(true);
    }

    private void preventBlockPlace(PlayerBlockPlaceEvent event) {
        event.setCancelled(true);
    }
}
