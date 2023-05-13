package net.hollowcube.world;

import net.hollowcube.world.util.Compress;
import net.hollowcube.world.dimension.DimensionTypes;
import net.hollowcube.world.event.PlayerInstanceLeaveEvent;
import net.hollowcube.world.util.FileUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.instance.AnvilLoader;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
    private final Instance instance;

    public BaseWorld(@NotNull WorldManager worldManager, @NotNull String id) {
        this(worldManager, id, new InstanceContainer(UUID.randomUUID(), DimensionTypes.FULL_BRIGHT));
    }

    public BaseWorld(@NotNull WorldManager worldManager, @NotNull String id, @NotNull Instance instance) {
        this.worldManager = worldManager;
        this.id = id;
        this.instance = instance;

        if (instance instanceof InstanceContainer instanceContainer)
            instanceContainer.setChunkLoader(new AnvilLoader(worldDir()));
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
            var regionDir = worldDir().resolve("region");
            Files.createDirectories(regionDir);
            Compress.unpack(regionDir, is);
        } catch (Exception e) {
            throw new RuntimeException("failed to load world data", e);
        }
    }

    @Override
    @Blocking
    public @NotNull String saveWorld() {
        try {
            instance.saveChunksToStorage();
            var compressed = Compress.pack(worldDir().toAbsolutePath());
            return worldManager.fileStorage().uploadFile(id, new ByteArrayInputStream(compressed), compressed.length);
        } catch (IOException e) {
            throw new RuntimeException("failed to compress world", e);
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
