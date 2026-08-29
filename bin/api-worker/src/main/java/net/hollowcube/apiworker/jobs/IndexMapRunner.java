package net.hollowcube.apiworker.jobs;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.MapFeaturesQueries.UpsertMapFeaturesParams;
import net.hollowcube.apiserver.job.IndexMap;
import net.hollowcube.apiworker.index.MapFeatures;
import net.hollowcube.apiworker.index.MapIndexer;
import net.hollowcube.apiworker.job.JobRunner;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.maps.MapClient;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.UUID;

/// Computes a map's `map_features` row from its world: fetch the world off the api-server, walk
/// it, write the row. Safe to repeat, since the row is a function of the world.
///
/// A map that is gone by the time its row is picked is dropped rather than retried; a world that
/// does not read at all is a failure, which after enough attempts is a parked row for a human,
/// where it belongs.
public final class IndexMapRunner implements JobRunner<IndexMap> {
    private static final Logger logger = LoggerFactory.getLogger(IndexMapRunner.class);

    private final ApiDatabase db;
    private final MapClient maps;

    public IndexMapRunner(ApiDatabase db, MapClient maps) {
        this.db = db;
        this.maps = maps;
        MapIndexer.init();
    }

    @Override
    public void run(@Nullable IndexMap data) {
        if (data == null) throw new IllegalArgumentException("index-map needs data: {\"mapId\": \"...\"}");
        final var mapId = UUID.fromString(data.mapId());

        final byte[] world;
        try {
            world = maps.getWorld(data.mapId());
        } catch (ApiClient.NotFoundError e) {
            logger.info("{} has no world to index ({}), dropping", mapId, data.reason());
            return;
        }

        final long start = System.nanoTime();
        final var features = MapIndexer.index(world);
        db.mapFeatures.upsertMapFeatures(new UpsertMapFeaturesParams(
            mapId, MapIndexer.FEATURE_VERSION, features.dataVersion(),
            features.blockCount(), features.extentX(), features.extentY(), features.extentZ(),
            features.occupiedCells(), features.distinctBlocks(), features.dominantBlockFrac(),
            features.entityCount(), features.textDisplayCount(),
            features.checkpointCount(), features.checkpointSpacing(), features.finishCount(), features.statusCount(),
            features.mechanics().stream().map(MapFeatures.Mechanic::id).toList(),
            List.copyOf(features.attributes()), List.copyOf(features.potionEffects()), List.copyOf(features.settings()),
            features.actionCount(), features.decodeFailures()));
        logger.info("indexed {} ({}) in {}ms: {}", mapId, data.reason(), (System.nanoTime() - start) / 1_000_000, features);
    }
}
