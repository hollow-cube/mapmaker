package net.hollowcube.apiserver.job;

import org.jetbrains.annotations.Nullable;

/// Asks for a map's `map_features` row to be computed from its world, as the data of an
/// [JobSpec#INDEX_MAP] row. Published when a map is created, republished or has its world updated,
/// and by a backfill when the feature version moves; or by hand:
///
/// ```sql
/// insert into jobs (job, instance, data)
/// values ('index-map', '<map id>', '{"mapId": "<map id>", "reason": "manual"}');
/// ```
///
/// Indexing is a pure function of the world bytes, so a duplicate is wasted work and nothing worse.
///
/// @param reason why, for the log; nothing reads it
public record IndexMap(String mapId, @Nullable String reason) {
}
