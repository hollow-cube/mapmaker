package net.hollowcube.mapmaker.instance.dimension;

import net.minestom.server.MinecraftServer;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.world.DimensionType;

public final class DimensionTypes {
    private DimensionTypes() {
    }

    public static final DimensionType FULL_BRIGHT = DimensionType.builder(NamespaceID.from("mapmaker:bright_dim"))
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

    public static final DimensionType MAPMAKER_MAP = DimensionType.builder(NamespaceID.from("mapmaker:map"))
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
        MinecraftServer.getDimensionTypeManager().addDimension(FULL_BRIGHT);
        MinecraftServer.getDimensionTypeManager().addDimension(MAPMAKER_MAP);
    }

}
