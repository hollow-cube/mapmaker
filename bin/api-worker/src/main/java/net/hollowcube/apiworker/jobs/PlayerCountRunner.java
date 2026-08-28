package net.hollowcube.apiworker.jobs;

import net.hollowcube.apiserver.common.PostHogIds;
import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiworker.job.JobRunner;
import net.hollowcube.posthog.PostHogClient;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/// The Go api-server's `count_reporter.go`: the number of open sessions, as a `player_count` event
/// under the internal distinct id, which is what the player count graphs read. Counts every row
/// the way the Go one did, hidden players included.
public final class PlayerCountRunner implements JobRunner<Void> {
    private static final Logger logger = LoggerFactory.getLogger(PlayerCountRunner.class);

    private final ApiDatabase db;
    private final PostHogClient posthog;

    public PlayerCountRunner(ApiDatabase db, PostHogClient posthog) {
        this.db = db;
        this.posthog = posthog;
    }

    @Override
    public void run(@Nullable Void data) {
        long count = db.sessions.countPlayerSessions();
        posthog.capture(PostHogIds.INTERNAL_ID, "player_count", Map.of("count", count));
        logger.info("reported {} online players", count);
    }
}
