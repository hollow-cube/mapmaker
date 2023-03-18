package net.hollowcube.world;

import net.hollowcube.world.compression.WorldCompressor;
import net.hollowcube.world.decompression.WorldDecompressor;
import net.hollowcube.world.dimension.DimensionTypes;
import net.hollowcube.world.event.PlayerInstanceLeaveEvent;
import net.hollowcube.world.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.world.util.FileUtil;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.event.instance.RemoveEntityFromInstanceEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.AnvilLoader;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

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

    protected @NotNull CompletableFuture<Void> loadWorld() {
        return worldManager.fileStorage().downloadFile(id)
                .thenApply(is -> {
                    try (is) {
                        var data = new byte[is.available()];
                        is.read(data);

                        var regionDir = worldDir().resolve("region");
                        Files.createDirectories(regionDir);
                        for (var region : WorldDecompressor.decompressWorldRegionFiles(data, 1024 * 1024 * 10)) {
                            Files.write(regionDir.resolve(region.getFileName()), region.getData(), StandardOpenOption.CREATE);
                        }
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    return null;
                });
    }

    @Override
    public @NotNull CompletableFuture<@NotNull String> saveWorld() {
        return instance.saveChunksToStorage()
                .thenCompose(result -> {
                    var compressed = WorldCompressor.compressWorldRegionFiles(worldDir().toAbsolutePath().toString(), null);
                    return worldManager.fileStorage().uploadFile(id,
                            new ByteArrayInputStream(compressed.getCompressedData()),
                            compressed.getCompressedData().length);
                });
    }

    @Override
    public @NotNull CompletableFuture<Void> unloadWorld() {
        MinecraftServer.getInstanceManager().unregisterInstance(instance);
        return CompletableFuture.runAsync(() -> {
            try {
                if (Files.exists(worldDir()))
                    FileUtil.deleteDirectory(worldDir());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, ForkJoinPool.commonPool());
    }

    private void handlePlayerSpawn(@NotNull PlayerSpawnEvent event) {
        if (event.getSpawnInstance().getUniqueId().equals(instance.getUniqueId())) {
            EventDispatcher.call(new PlayerSpawnInInstanceEvent(event.getPlayer()));
        }
    }

    private void handleEntityRemoved(@NotNull RemoveEntityFromInstanceEvent event) {
        if (event.getEntity() instanceof Player player) {
            EventDispatcher.call(new PlayerInstanceLeaveEvent(player, event.getInstance()));
        }
    }
}
