package net.hollowcube.ipc.map;

import org.jetbrains.annotations.Nullable;

import java.time.Instant;

/// Where a map is on the way to being published.
public sealed interface MapStatus permits
    MapStatus.Draft, MapStatus.Verifying, MapStatus.ReadyToPublish, MapStatus.Published,
    MapStatus.NotFound, MapStatus.Unknown {

    record Draft(PublishReadiness readiness) implements MapStatus {}

    /// The editor has drained and a verification run is awaited.
    record Verifying(PublishReadiness readiness) implements MapStatus {}

    record ReadyToPublish() implements MapStatus {}

    record Published(String publishedId, Instant publishedAt) implements MapStatus {}

    record NotFound() implements MapStatus {}

    record Unknown(@Nullable String type) implements MapStatus {}
}
