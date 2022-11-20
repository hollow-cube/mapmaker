package net.hollowcube.mapmaker.dev;

import net.hollowcube.mapmaker.hub.MapHandle;
import net.hollowcube.mapmaker.hub.handler.MapOrchestrator;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

public class LocalMapOrchestrator implements MapOrchestrator {

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

    private final Map<String, Instance> mapInstances = new ConcurrentHashMap<>();

    @Override
    public @NotNull CompletableFuture<MapHandle> openMap(@NotNull MapData map, int flags) {
        var instance = MinecraftServer.getInstanceManager().createInstanceContainer(BRIGHT_DIMENSION);
//        instance.setGenerator(unit -> unit.modifier().fill(new Vec(-5, 39, -5), new Vec(5, 40, 5), Block.STONE));
        instance.setBlock(0, 58, 0, Block.WHITE_WOOL);
        instance.getWorldBorder().setDiameter(100);
        var handle = new Handle(UUID.randomUUID().toString(), map.id(), flags, instance);
        mapInstances.put(map.id(), instance);
        return CompletableFuture.completedFuture(handle);
    }

    @Override
    public @NotNull CompletableFuture<Void> joinMap(@NotNull Player player, @NotNull MapHandle anyHandle) {
        if (!(anyHandle instanceof Handle handle)) {
            return CompletableFuture.failedFuture(new IllegalArgumentException("unsupported map handle: " + anyHandle.getClass().getName()));
        }

        var instance = handle.instance();

        return player.setInstance(instance, new Pos(0, 60, 0));
    }

    public record Handle(
            @NotNull String id,
            @NotNull String mapId,
            int flags,
            @NotNull Instance instance
    ) implements MapHandle {
    }

}
