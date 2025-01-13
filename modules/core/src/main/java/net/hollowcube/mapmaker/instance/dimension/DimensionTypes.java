package net.hollowcube.mapmaker.instance.dimension;

import net.hollowcube.mapmaker.map.MapSize;
import net.minestom.server.MinecraftServer;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;
import org.jetbrains.annotations.NotNull;

public final class DimensionTypes {
    private DimensionTypes() {
    }

    public static final DynamicRegistry.Key<DimensionType> FULL_BRIGHT;
    public static final DynamicRegistry.Key<DimensionType> TALL_2K;
    public static final DynamicRegistry.Key<DimensionType> TALL_4K;

    public static @NotNull DynamicRegistry.Key<DimensionType> forSize(MapSize size, boolean hasLighting) {
        return switch (size) {
            // todo support lighting on these tall ones in the future
            case TALL_2K -> TALL_2K;
            case TALL_4K -> TALL_4K;
            default -> hasLighting ? DimensionType.OVERWORLD : FULL_BRIGHT;
        };
    }

    static {
        var registry = MinecraftServer.getDimensionTypeRegistry();
        FULL_BRIGHT = registry.register(NamespaceID.from("mapmaker:bright_dim"), DimensionType.builder()
                .ambientLight(2.0f)
                .build());
        TALL_2K = registry.register(NamespaceID.from("mapmaker:bright_dim_2k"), DimensionType.builder()
                .minY(-64)
                .height(960)
                .ambientLight(2.0f)
                .build());
        TALL_4K = registry.register(NamespaceID.from("mapmaker:bright_dim_4k"), DimensionType.builder()
                .minY(-2032)
                .height(2016)
                .ambientLight(2.0f)
                .build());
    }

}
