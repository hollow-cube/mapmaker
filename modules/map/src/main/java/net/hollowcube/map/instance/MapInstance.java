package net.hollowcube.map.instance;

import net.hollowcube.map.event.PlayerSpawnInInstanceEvent;
import net.hollowcube.mapmaker.map.MapManager;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.util.DimensionUtil;
import net.hollowcube.mapmaker.util.TagUtil;
import net.minestom.server.entity.GameMode;
import net.minestom.server.event.player.PlayerBlockBreakEvent;
import net.minestom.server.event.player.PlayerBlockPlaceEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class MapInstance {
    public static final Tag<String> MAP_ID = Tag.String("mapmaker:map/id");
    public static final Tag<MapData> MAP_DATA = TagUtil.noop("mapmaker:map/data");

    public static final int FLAG_EDIT = 0x1;

    public static @NotNull CompletableFuture<@NotNull Instance> create(@NotNull MapData map, int flags) {
        var instance = new InstanceContainer(UUID.randomUUID(), DimensionUtil.FULL_BRIGHT);
        instance.setBlock(0, 58, 0, Block.WHITE_WOOL);
        instance.getWorldBorder().setDiameter(100); //todo

        instance.setTag(MAP_ID, map.getId());
        instance.setTag(MAP_DATA, map);

        var eventNode = instance.eventNode();
        // Extra options depending on whether this instance is for editing or not
        if ((flags & FLAG_EDIT) == 0) {
            // Playing
            eventNode.addListener(PlayerBlockBreakEvent.class, MapInstance::preventBlockBreak);
            eventNode.addListener(PlayerBlockPlaceEvent.class, MapInstance::preventBlockPlace);
        } else {
            // Editing
            eventNode.addListener(PlayerSpawnInInstanceEvent.class, MapInstance::initPlayerForEditing);
        }

        return CompletableFuture.completedFuture(instance);
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

}
