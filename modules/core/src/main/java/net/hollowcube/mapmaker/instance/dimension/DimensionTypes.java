package net.hollowcube.mapmaker.instance.dimension;

import net.minestom.server.MinecraftServer;
import net.minestom.server.ServerProcess;
import net.minestom.server.registry.RegistryKey;
import net.minestom.server.world.DimensionType;
import net.minestom.server.world.attribute.EnvironmentAttribute;
import org.jetbrains.annotations.TestOnly;

import java.util.Objects;

import static net.kyori.adventure.key.Key.key;

public final class DimensionTypes {
    private DimensionTypes() {
    }

    public static final RegistryKey<DimensionType> FULL_BRIGHT;

    public static final RegistryKey<DimensionType> MAPMAKER_MAP;

    static {
        var registry = MinecraftServer.getDimensionTypeRegistry();
        var overworld = Objects.requireNonNull(registry.get(key("overworld")), "overworld");
        FULL_BRIGHT = registry.register("mapmaker:bright_dim",
            copyDimension(overworld)
                .ambientLight(1.0f)
                .build());
        MAPMAKER_MAP = registry.register("mapmaker:map",
            copyDimension(overworld)
                .ambientLight(1.0f)
                .build());
    }

    @TestOnly
    public static void register(ServerProcess process) {
        var overworld = Objects.requireNonNull(process.dimensionType().get(key("overworld")), "overworld");
        process.dimensionType().register("mapmaker:bright_dim",
            copyDimension(overworld)
                .ambientLight(1.0f)
                .build());
        process.dimensionType().register("mapmaker:map",
            copyDimension(overworld)
                .ambientLight(1.0f)
                .build());
    }

    private static DimensionType.Builder copyDimension(DimensionType type) {
        var builder = DimensionType.builder()
            .fixedTime(type.hasFixedTime())
            .skylight(type.hasSkylight())
            .ceiling(type.hasCeiling())
            .coordinateScale(type.coordinateScale())
            .minY(type.minY())
            .height(type.height())
            .logicalHeight(type.logicalHeight())
            .infiniburn(type.infiniburn())
            .ambientLight(type.ambientLight())
            .monsterSpawnLightLevel(type.monsterSpawnLightLevel())
            .monsterSpawnBlockLightLimit(type.monsterSpawnBlockLightLimit())
            .skybox(type.skybox())
            .cardinalLight(type.cardinalLight())
            .timelines(type.timelines());
        for (var entry : type.attributes().entries().entrySet()) {
            //noinspection unchecked
            builder.setAttribute((EnvironmentAttribute<Object>) entry.getKey(), entry.getValue().argument());
        }
        return builder;
    }

}
