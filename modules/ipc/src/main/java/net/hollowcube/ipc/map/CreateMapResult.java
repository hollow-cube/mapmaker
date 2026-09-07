package net.hollowcube.ipc.map;

import org.jetbrains.annotations.Nullable;

/// What creating a map answered.
public sealed interface CreateMapResult {

    record Success(MapData map) implements CreateMapResult {}

    /// @param limit how many unpublished maps the owner may hold, all of which are taken
    record NoSlots(int limit) implements CreateMapResult {}

    record SizeLocked() implements CreateMapResult {}

    /// No such owner.
    record NotFound() implements CreateMapResult {}

    record Unknown(@Nullable String type) implements CreateMapResult {}
}
