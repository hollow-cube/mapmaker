package net.hollowcube.terraform.entity;

import net.hollowcube.terraform.util.TypedData;
import net.minestom.server.coordinate.Pos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/// Makes the entity `id` match `data` at `pos`:
/// - spawn if absent
/// - despawn if null `data`
/// - update if present
/// - respawns it if type differs
///
/// `pos` always set so the edit can be clipped
public record EntityEdit(UUID id, Pos pos, @Nullable TypedData data) {

    public static EntityEdit remove(UUID id, Pos pos) {
        return new EntityEdit(id, pos, null);
    }
}
