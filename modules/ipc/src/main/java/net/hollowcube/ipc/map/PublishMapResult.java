package net.hollowcube.ipc.map;

import org.jetbrains.annotations.Nullable;

/// What publishing a map answered.
public sealed interface PublishMapResult permits
    PublishMapResult.Success, PublishMapResult.NotFound, PublishMapResult.Blocked,
    PublishMapResult.Unknown {

    /// Published, now or by an earlier call: the same map comes back on a retry.
    record Success(MapData map) implements PublishMapResult {}

    record NotFound() implements PublishMapResult {}

    record Blocked(PublishReadiness readiness) implements PublishMapResult {}

    record Unknown(@Nullable String type) implements PublishMapResult {}
}
