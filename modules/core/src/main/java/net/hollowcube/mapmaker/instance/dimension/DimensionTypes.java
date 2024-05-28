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
        FULL_BRIGHT = registry.register(DimensionType.builder(NamespaceID.from("mapmaker:bright_dim"))
                .ambientLight(2.0f)
                .build());
        MAPMAKER_MAP = registry.register(DimensionType.builder(NamespaceID.from("mapmaker:map"))
                .ambientLight(2.0f)
                .build());
    }

}
