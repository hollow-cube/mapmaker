package net.hollowcube.mapmaker.instance.dimension;

import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

public final class DimensionTypes {
    private DimensionTypes() {
    }

    public static final DynamicRegistry.Key<DimensionType> FULL_BRIGHT;

    public static final DynamicRegistry.Key<DimensionType> MAPMAKER_MAP;

    static {
        var registry = MinecraftServer.getDimensionTypeRegistry();
        FULL_BRIGHT = registry.register("mapmaker:bright_dim", DimensionType.builder()
                .ambientLight(2.0f)
                .build());
        MAPMAKER_MAP = registry.register("mapmaker:map", DimensionType.builder()
                .ambientLight(2.0f)
                .build());
    }

    @TestOnly
    public static void register(@NotNull ServerProcess process) {
        process.dimensionType().register("mapmaker:bright_dim", DimensionType.builder()
                .ambientLight(2.0f)
                .build());
        process.dimensionType().register("mapmaker:map", DimensionType.builder()
                .ambientLight(2.0f)
                .build());
    }

}
