package net.hollowcube.terraform.entity;

import net.hollowcube.terraform.util.Transform;
import net.hollowcube.terraform.util.TypedData;
import net.minestom.server.coordinate.Area;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

/// Host extension for manipulating entities.
/// Calls occur on the apply thread so should not block.
@SuppressWarnings("UnstableApiUsage")
public interface EntityAccessor {
    /// The entity's current state as an edit setting it, or null if the instance has no such entity.
    @Nullable EntityEdit capture(Instance instance, UUID id);

    void apply(Instance instance, UUID id, Pos pos, TypedData data);

    void spawn(Instance instance, UUID id, Pos pos, TypedData data);

    void remove(Instance instance, UUID id);

    /// Entities inside the box excluding players and passengers (they are captured in their parent)
    List<EntityEdit> entitiesIn(Instance instance, Area.Cuboid box);

    default TypedData copy(TypedData data, Transform transform) {
        return data;
    }
}
