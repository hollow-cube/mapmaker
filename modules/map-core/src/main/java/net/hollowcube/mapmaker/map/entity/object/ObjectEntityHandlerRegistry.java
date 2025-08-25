package net.hollowcube.mapmaker.map.entity.object;

import net.hollowcube.mapmaker.map.entity.interaction.InteractionEntity;
import net.hollowcube.mapmaker.map.entity.marker.MarkerEntity;
import net.hollowcube.mapmaker.map.entity.marker.builtin.ParticleEmitterMarkerHandler;
import net.hollowcube.mapmaker.map.entity.object.builtin.TeleportObjectHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ObjectEntityHandlerRegistry {
    private final Map<String, Function<ObjectEntity, ObjectEntityHandler>> factories = new HashMap<>();
    private final Map<String, ObjectEntityEditor> editors = new HashMap<>();
    private final Map<Class<?>, ObjectEntityEditor> defaultEditors = new HashMap<>();

    public ObjectEntityHandlerRegistry() {
        registerForMarkers(ParticleEmitterMarkerHandler.ID, ParticleEmitterMarkerHandler::new);
        registerForInteractions(TeleportObjectHandler.ID, TeleportObjectHandler::new);
    }

    public void registerForMarkers(String id, Function<MarkerEntity, ObjectEntityHandler> factory) {
        factories.put(id, entity -> entity instanceof MarkerEntity marker ? factory.apply(marker) : null);
    }

    public void registerForInteractions(String id, Function<InteractionEntity, ObjectEntityHandler> factory) {
        factories.put(id, entity -> entity instanceof InteractionEntity interaction ? factory.apply(interaction) : null);
    }

    public void register(String id, Function<ObjectEntity, ObjectEntityHandler> factory) {
        factories.put(id, factory);
    }

    public void registerEditor(String id, ObjectEntityEditor editor) {
        editors.put(id, editor);
    }

    public void registerDefaultEditor(Class<?> objectClass, ObjectEntityEditor editor) {
        defaultEditors.put(objectClass, editor);
    }

    public @Nullable ObjectEntityHandler create(@Nullable String type, @NotNull ObjectEntity entity) {
        if (type == null) return null;
        var factory = factories.get(type);
        if (factory == null) return null;
        return factory.apply(entity);
    }

    public @Nullable ObjectEntityEditor getEditor(@NotNull String type) {
        return this.editors.get(type);
    }

    public @Nullable ObjectEntityEditor getEditor(@NotNull String type, @Nullable Class<?> owner) {
        var editor = this.getEditor(type);
        return editor != null || owner == null ? editor : this.defaultEditors.get(owner);
    }
}
