package net.hollowcube.mapmaker.dev.unleash;

import io.getunleash.UnleashContext;
import io.getunleash.strategy.Strategy;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.Set;

public class MapIdStrategy implements Strategy {
    @Override
    public @NotNull String getName() {
        return "MapIDs";
    }

    @Override
    public boolean isEnabled(@NotNull Map<String, String> parameters) {
        return false;
    }

    @Override
    public boolean isEnabled(@NotNull Map<String, String> params, @NotNull UnleashContext context) {
        var options = Set.of(params.getOrDefault("mapId", "").split(","));
        var mapIdParam = context.getProperties().getOrDefault("mapId", "");
        return options.contains(mapIdParam);
    }
}
