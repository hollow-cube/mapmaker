package net.hollowcube.apiworker.jobs;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiworker.job.JobRunner;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;

public final class MapStatsRunner implements JobRunner<Void> {
    private static final Logger logger = LoggerFactory.getLogger(MapStatsRunner.class);

    private static final Duration OVERLAP = Duration.ofMinutes(1);
    private static final int BATCH = 2000;

    private final ApiDatabase db;
    private final boolean full;

    public MapStatsRunner(ApiDatabase db, boolean full) {
        this.db = db;
        this.full = full;
    }

    @Override
    public void run(@Nullable Void data) {
        var last = db.mapStats.lastRefresh();
        var ids = full || last == null
            ? db.mapStats.listAll()
            : db.mapStats.changedSince(last.minus(OVERLAP));
        for (int i = 0; i < ids.size(); i += BATCH)
            db.mapStats.refresh(ids.subList(i, Math.min(i + BATCH, ids.size())));
        if (!ids.isEmpty()) logger.info("refreshed stats for {} maps", ids.size());
    }
}
