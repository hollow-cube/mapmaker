package net.hollowcube.mapmaker.instance.dimension;

import net.minestom.server.MinecraftServer;
import net.minestom.server.registry.DynamicRegistry;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;

public final class DimensionTypes {
    private DimensionTypes() {
    }

    public static final DynamicRegistry.Key<DimensionType> FULL_BRIGHT;

    public static final DynamicRegistry.Key<DimensionType> MAPMAKER_MAP;

    static {
        var registry = MinecraftServer.getDimensionTypeRegistry();
        FULL_BRIGHT = registry.register(NamespaceID.from("mapmaker:bright_dim"), DimensionType.builder()
                .ambientLight(2.0f)
                .build());
        MAPMAKER_MAP = registry.register(NamespaceID.from("mapmaker:map"), DimensionType.builder()
                .ambientLight(2.0f)
                .build());
    }

}
