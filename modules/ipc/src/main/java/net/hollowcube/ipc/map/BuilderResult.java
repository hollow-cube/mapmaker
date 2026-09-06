package net.hollowcube.ipc.map;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/// What became of an invitation, acceptance, rejection or removal.
public sealed interface BuilderResult permits
    BuilderResult.Success, BuilderResult.AlreadyDone, BuilderResult.NoSlots,
    BuilderResult.CapacityReached, BuilderResult.InvitesDisabled, BuilderResult.AlreadyBuilder,
    BuilderResult.InviteGone, BuilderResult.MapNotFound, BuilderResult.PlayerNotFound,
    BuilderResult.MapPublished, BuilderResult.Owner, BuilderResult.Unknown {

    /// Whether the builders are as asked, whether this call or an earlier one made them so.
    default boolean succeeded() {
        return builders() != null;
    }

    /// Everyone but the owner as they stand after the call, or null when the map was not touched.
    default @Nullable List<MapBuilder> builders() {
        return switch (this) {
            case Success(var builders) -> builders;
            case AlreadyDone(var builders) -> builders;
            default -> null;
        };
    }

    /// @param builders everyone but the owner, as they stand after the call
    record Success(List<MapBuilder> builders) implements BuilderResult {}

    /// An earlier call already did this, so a retry has nothing left to do.
    record AlreadyDone(List<MapBuilder> builders) implements BuilderResult {}

    /// The accepting player has every one of their map slots in use.
    record NoSlots(int limit) implements BuilderResult {}

    /// The map has as many builders as its owner is allowed.
    record CapacityReached(int limit) implements BuilderResult {}

    record InvitesDisabled() implements BuilderResult {}

    /// The player already accepted: not to be invited again, and too late to reject.
    record AlreadyBuilder() implements BuilderResult {}

    /// There is no invitation to accept.
    record InviteGone() implements BuilderResult {}

    record MapNotFound() implements BuilderResult {}

    record PlayerNotFound() implements BuilderResult {}

    record MapPublished() implements BuilderResult {}

    /// The owner is not a builder to be added or removed.
    record Owner() implements BuilderResult {}

    record Unknown(@Nullable String type) implements BuilderResult {}
}
