-- name: upsertMapFeatures :exec
-- The whole row, replaced: a map's features are a function of its world, so there is nothing in
-- an old row worth keeping. Parameters cannot be null, so the two nullable columns are nulled
-- here: a data version of -1 is a world that predates one, and spacing means nothing under two
-- checkpoints.
insert into map_features (map_id, feature_version, data_version,
                          block_count, extent_x, extent_y, extent_z, occupied_cells, distinct_blocks, dominant_block_frac,
                          entity_count, text_display_count,
                          checkpoint_count, checkpoint_spacing, finish_count, status_count,
                          mechanics, attributes, potion_effects, settings, action_count, decode_failures)
values ($mapId, $featureVersion, nullif($dataVersion, -1),
        $blockCount, $extentX, $extentY, $extentZ, $occupiedCells, $distinctBlocks, $dominantBlockFrac,
        $entityCount, $textDisplayCount,
        $checkpointCount, case when $checkpointCount >= 2 then $checkpointSpacing::double precision end, $finishCount, $statusCount,
        $mechanics, $attributes, $potionEffects, $settings, $actionCount, $decodeFailures)
on conflict (map_id) do update
    set feature_version     = excluded.feature_version,
        indexed_at          = now(),
        data_version        = excluded.data_version,
        block_count         = excluded.block_count,
        extent_x            = excluded.extent_x,
        extent_y            = excluded.extent_y,
        extent_z            = excluded.extent_z,
        occupied_cells      = excluded.occupied_cells,
        distinct_blocks     = excluded.distinct_blocks,
        dominant_block_frac = excluded.dominant_block_frac,
        entity_count        = excluded.entity_count,
        text_display_count  = excluded.text_display_count,
        checkpoint_count    = excluded.checkpoint_count,
        checkpoint_spacing  = excluded.checkpoint_spacing,
        finish_count        = excluded.finish_count,
        status_count        = excluded.status_count,
        mechanics           = excluded.mechanics,
        attributes          = excluded.attributes,
        potion_effects      = excluded.potion_effects,
        settings            = excluded.settings,
        action_count        = excluded.action_count,
        decode_failures     = excluded.decode_failures;

-- name: getMapFeatures :one
select map_features.*
from map_features
where map_id = $mapId;
