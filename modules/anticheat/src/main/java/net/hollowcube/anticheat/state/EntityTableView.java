package net.hollowcube.anticheat.state;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/// An immutable view of an [EntityTable] at one instant. Its map is the table's own storage, which
/// the table copies before its next write.
public record EntityTableView(Int2ObjectMap<TrackedEntity> entities, TrackedEntity player) {

    public int size() {
        return entities.size();
    }

    public Collection<TrackedEntity> all() {
        return entities.values();
    }

    public @Nullable TrackedEntity get(int entityId) {
        return entities.get(entityId);
    }
}
