package net.hollowcube.world;

import net.hollowcube.polar.PolarLoader;
import net.hollowcube.polar.PolarWorld;
import net.hollowcube.polar.PolarWriter;
import net.hollowcube.world.dimension.DimensionTypes;
import net.hollowcube.world.event.PlayerInstanceLeaveEvent;
import net.hollowcube.world.util.FileUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.LightingChunk;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public class BaseWorld implements World {
    private static final Path WORLD_DIR;

    static {
        var worldDir = System.getProperty("hc.instance.temp_dir");
        if (worldDir != null) {
            WORLD_DIR = Path.of(worldDir);
        } else {
            try {
                WORLD_DIR = Files.createTempDirectory("minestom-worlds");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private final WorldManager worldManager;
    private final String id;
    private final InstanceContainer instance;

    public BaseWorld(@NotNull WorldManager worldManager, @NotNull String id) {
        this(worldManager, id, new InstanceContainer(UUID.randomUUID(), DimensionTypes.FULL_BRIGHT));
    }

    public BaseWorld(@NotNull WorldManager worldManager, @NotNull String id, @NotNull InstanceContainer instance) {
        this.worldManager = worldManager;
        this.id = id;

        this.instance = instance;
        this.instance.setChunkLoader(new PolarLoader(new PolarWorld(
                PolarWorld.LATEST_VERSION,
                PolarWorld.CompressionType.ZSTD,
                (byte) -4, (byte) 19,
                new ArrayList<>()
        )));
//        this.instance.setChunkSupplier(LightingChunk::new);
        MinecraftServer.getInstanceManager().registerInstance(instance);

        var eventNode = instance.eventNode();
        eventNode.addListener(RemoveEntityFromInstanceEvent.class, this::handleEntityRemoved);
    }

    @Override
    public @NotNull String id() {
        return id;
    }

    @Override
    public @NotNull Instance instance() {
        return instance;
    }

    protected @NotNull Path worldDir() {
        return WORLD_DIR.resolve(id);
    }

    @Blocking
    public void loadWorld() {
        var file = worldManager.fileStorage().downloadFile(id);
        try (var is = file.data()) {
            instance.setChunkLoader(new PolarLoader(is));
//            var regionDir = worldDir().resolve("region");
//            Files.createDirectories(regionDir);
//            Compress.unpack(regionDir, is);
        } catch (Exception e) {
            throw new RuntimeException("failed to load world data", e);
        }
    }

    @Override
    @Blocking
    public @NotNull String saveWorld() {
        try {
            instance.saveChunksToStorage();
            var polarWorld = ((PolarLoader) instance.getChunkLoader()).world();
            var polarData = PolarWriter.write(polarWorld);
            return worldManager.fileStorage()
                    .uploadFile(id, new ByteArrayInputStream(polarData), polarData.length);
//            var compressed = Compress.pack(worldDir().toAbsolutePath());
//            return worldManager.fileStorage().uploadFile(id, new ByteArrayInputStream(compressed), compressed.length);
        } catch (Exception e) {
            throw new RuntimeException("failed to save world data", e);
        }

    }

    @Override
    @Blocking
    public void unloadWorld() {
        MinecraftServer.getInstanceManager().unregisterInstance(instance);
        try {
            if (Files.exists(worldDir()))
                FileUtil.deleteDirectory(worldDir());
        } catch (Exception e) {
            throw new RuntimeException("failed to delete world dir", e);
        }
    }

    private void handleEntityRemoved(@NotNull RemoveEntityFromInstanceEvent event) {
        if (event.getEntity() instanceof Player player) {
            EventDispatcher.call(new PlayerInstanceLeaveEvent(player, event.getInstance()));
        }
    }
}
