package net.hollowcube.mapmaker.map.entity.marker;

import net.hollowcube.mapmaker.map.entity.marker.builtin.ParticleEmitterMarkerHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class MarkerHandlerRegistry {
    private final Map<String, Function<MarkerEntity, MarkerHandler>> factories = new HashMap<>();

    public MarkerHandlerRegistry() {
        register(ParticleEmitterMarkerHandler.ID, ParticleEmitterMarkerHandler::new);
    }

    public void register(String id, Function<MarkerEntity, MarkerHandler> factory) {
        factories.put(id, factory);
    }

    public @Nullable MarkerHandler create(@Nullable String type, @NotNull MarkerEntity entity) {
        if (type == null) return null;
        var factory = factories.get(type);
        if (factory == null) return null;
        return factory.apply(entity);
    }

}
