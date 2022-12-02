package net.hollowcube.map.instance;

import net.hollowcube.database.FileStorage;
import net.hollowcube.database.FileStorageS3;
import net.hollowcube.map.event.PlayerInstanceLeaveEvent;
import net.hollowcube.map.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.map.util.FileUtil;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.util.DimensionUtil;
import net.hollowcube.mapmaker.util.StaticAbuse;
import net.hollowcube.mapmaker.util.TagUtil;
import net.hollowcube.world.compression.WorldCompressor;
import net.hollowcube.world.decompression.WorldDecompressor;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.instance.AnvilLoader;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MapInstance {
    public static final Tag<String> MAP_ID = Tag.String("mapmaker:map/id");
    public static final Tag<MapData> MAP_DATA = TagUtil.noop("mapmaker:map/data");

    public static final int FLAG_EDIT = 0x1;

    public static @NotNull CompletableFuture<@NotNull Instance> create(@NotNull MapData map, int flags) {
        var instance = new InstanceContainer(UUID.randomUUID(), DimensionUtil.FULL_BRIGHT);
        instance.getWorldBorder().setDiameter(100); //todo
        instance.setChunkLoader(new AnvilLoader("world/" + map.getId()));

        instance.setTag(MAP_ID, map.getId());
        instance.setTag(MAP_DATA, map);

        var eventNode = instance.eventNode();
        eventNode.addListener(PlayerInstanceLeaveEvent.class, MapInstance::handlePlayerLeave);
        // Extra options depending on whether this instance is for editing or not
        if ((flags & FLAG_EDIT) == 0) {
            // Playing
            eventNode.addListener(PlayerBlockBreakEvent.class, MapInstance::preventBlockBreak);
            eventNode.addListener(PlayerBlockPlaceEvent.class, MapInstance::preventBlockPlace);
        } else {
            // Editing
            eventNode.addListener(PlayerSpawnInInstanceEvent.class, MapInstance::initPlayerForEditing);
        }

        // Load world
        if (map.getMapFileId() == null) {
            // No map file, create an empty world
            instance.setBlock(0, 58, 0, Block.WHITE_WOOL);
            return CompletableFuture.completedFuture(instance);
        } else {
            return storage.downloadFile(map.getMapFileId())
                    .thenApply(is -> {
                        try (is) {
                            var data = new byte[is.available()];
                            is.read(data);

                            var basePath = Path.of("world/" + map.getId() + "/region");
                            Files.createDirectories(basePath);
                            for (var region : WorldDecompressor.decompressWorldRegionFiles(data, 1024 * 1024 * 10)) {
                                Files.write(basePath.resolve(region.getFileName()), region.getData(), StandardOpenOption.CREATE);
                            }

                            return instance;
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    });
        }

//        try {
//            var data = storage.downloadFile(map.getId() + "/v0").join();
//            var compressedData = new byte[data.available()];
//            data.read(compressedData);
//            data.close();
//            var regions = WorldDecompressor.decompressWorldRegionFiles(compressedData, 1024 * 1024 * 10);
//
//            var basePath = Path.of("world/" + map.getId());
//            Files.createDirectories(basePath);
//            for (var region : regions) {
//                Files.write(basePath.resolve(region.getFileName()), region.getData(), StandardOpenOption.CREATE);
//            }
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

//        return CompletableFuture.completedFuture(instance);
    }

    private static void preventBlockBreak(PlayerBlockBreakEvent event) {
        event.setCancelled(true);
    }

    private static void preventBlockPlace(PlayerBlockPlaceEvent event) {
        event.setCancelled(true);
    }

    private static void initPlayerForEditing(@NotNull PlayerSpawnInInstanceEvent event) {
        var player = event.getPlayer();
        player.setGameMode(GameMode.CREATIVE);

        var instance = event.getInstance();
        var map = Objects.requireNonNull(instance.getTag(MAP_DATA));
        player.sendMessage("Now editing " + map.getName());
    }

    private static final FileStorage storage = FileStorageS3.connect(
            "http://localhost:9000/",
            "DTprdE3DBZ7vG8wQ",
            "qByxgkPV7rO7zo12KmRUkikSBMwYJCRj"
    );


    private static void handlePlayerLeave(@NotNull PlayerInstanceLeaveEvent event) {
        System.out.println("LEFT INSTANCE");

        // Attempt to save instance
        var instance = event.getInstance();
        var map = Objects.requireNonNull(instance.getTag(MAP_DATA));
        instance.saveChunksToStorage()
                .thenCompose(result -> {
                    var compressed = WorldCompressor.compressWorldRegionFiles("world/" + map.getId(), null);
                    return storage.uploadFile(map.getId() + "/v0", new ByteArrayInputStream(compressed.getCompressedData()), compressed.getCompressedData().length);
                })
                .thenCompose(fileId -> {
                    map.setMapFileId(fileId);
                    return StaticAbuse.mapStorage.updateMap(map);
                })
                .thenAccept(unused -> {
//                    try {
//                        FileUtil.deleteDirectory(Path.of("world/" + map.getId()));
//                    } catch (Exception e) {
//                        throw new RuntimeException(e);
//                    }
                    System.out.println("Saved instance: " + map.getId());
                    MinecraftServer.getInstanceManager().unregisterInstance(instance);
                });
    }

}
