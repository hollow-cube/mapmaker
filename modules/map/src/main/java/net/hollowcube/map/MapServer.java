package net.hollowcube.map;

import net.hollowcube.map.command.HubCommand;
import net.hollowcube.map.command.MapDebugCommand;
import net.hollowcube.mapmaker.map.MapHandle;
import net.hollowcube.mapmaker.map.MapManager;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.tag.Tag;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Wrapper for managing maps.
 *
 * Will eventually act as the api for interacting with other hub and map nodes. Will need some refactoring
 */
public class MapServer implements MapManager {
    public static final Tag<Boolean> MAP_MARKER = Tag.Boolean("mapmaker:map_marker");

    private static final DimensionType BRIGHT_DIMENSION = DimensionType.builder(NamespaceID.from("mapmaker:bright"))
            .ultrawarm(false)
            .natural(true)
            .piglinSafe(false)
            .respawnAnchorSafe(false)
            .bedSafe(true)
            .raidCapable(true)
            .skylightEnabled(true)
            .ceilingEnabled(false)
            .fixedTime(null)
            .ambientLight(2.0f)
            .height(384)
            .minY(-64)
            .logicalHeight(384)
            .infiniburn(NamespaceID.from("minecraft:infiniburn_overworld"))
            .build();

    static {
        MinecraftServer.getDimensionTypeManager().addDimension(BRIGHT_DIMENSION);
    }

    public final Map<String, WeakReference<Instance>> instances = new HashMap<>();

    public MapServer() {
        var commandManager = MinecraftServer.getCommandManager();
        commandManager.register(new MapDebugCommand(this));
        commandManager.register(new HubCommand());
    }

    @Override
    public @NotNull CompletableFuture<Void> joinMap(@NotNull MapData map, int flags, @NotNull Player player) {
        var instance = MinecraftServer.getInstanceManager().createInstanceContainer(BRIGHT_DIMENSION);
        instance.setBlock(0, 58, 0, Block.WHITE_WOOL);
        instance.getWorldBorder().setDiameter(100);
        instance.setTag(MAP_MARKER, true);

        instances.put(map.id(), new WeakReference<>(instance));

        return player.setInstance(instance, new Pos(0, 60, 0))
                .thenAccept(unused -> player.refreshCommands());
    }

    private void handleInstanceLeave() {

    }

}
