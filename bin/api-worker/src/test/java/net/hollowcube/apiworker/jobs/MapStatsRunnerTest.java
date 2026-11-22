package net.hollowcube.apiworker.jobs;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.MapStats;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Objects;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class MapStatsRunnerTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("../../modules/api/src/main/sql/migrations");

    private final ApiDatabase db = TEST_DB.database(ApiDatabase::new);

    private UUID map() {
        var id = UUID.randomUUID();
        TEST_DB.seed(
            """
            insert into maps (id, owner, created_at, updated_at, name, icon, variant, spawn_point, size_class,
                              verification_state, quality)
            values ('%s', '%s', now(), now(), '', '', 'parkour', '{}', 'normal', 'unverified', 'unrated')"""
                .formatted(id, UUID.randomUUID())
        );
        return id;
    }

    private void run(
        UUID map,
        UUID player,
        String type,
        boolean completed,
        long playtime,
        String age
    ) {
        TEST_DB.seed(
            """
            insert into save_states (id, map_id, player_id, type, completed, playtime, total_playtime, state,
                                     created_at, updated_at)
            values ('%s', '%s', '%s', '%s', %s, %s, %s, '{}', now() - interval '%s', now() - interval '%s')"""
                .formatted(
                    UUID.randomUUID(),
                    map,
                    player,
                    type,
                    completed,
                    playtime,
                    playtime,
                    age,
                    age
                )
        );
    }

    private MapStats stats(UUID map) {
        return Objects.requireNonNull(db.maps.getStats(map));
    }

    @Test
    void firstRun_refreshesEveryMap() {
        var map = map();
        var author = UUID.randomUUID();
        var fast = UUID.randomUUID();
        var slow = UUID.randomUUID();
        run(map, author, "verifying", true, 60_000, "30 days");
        run(map, fast, "playing", true, 30_000, "20 days");
        run(map, fast, "playing", true, 10_000, "1 day");
        run(map, slow, "playing", false, 5_000, "20 days");
        run(map, slow, "playing", true, 90_000, "10 days");
        run(map, UUID.randomUUID(), "playing", false, 1_000, "1 hour");

        new MapStatsRunner(db, false).run(null);

        var stats = stats(map);
        assertEquals(3, stats.playCount(), "the author's verifying run is not a play");
        assertEquals(2, stats.winCount());
        // Two first clears, 30s and 90s (not the later 10s), against a 60s verify.
        var blended = Math.exp((10 * Math.log(60_000 * 1.03) + 2 * Math.log(60_000)) / 12);
        assertEquals(blended, Objects.requireNonNull(stats.estLengthMs()), 1);
        assertNotNull(stats.refreshedAt());
    }

    @Test
    void laterRuns_refreshOnlyChangedMaps() {
        var quiet = map();
        var busy = map();
        run(quiet, UUID.randomUUID(), "playing", true, 1_000, "2 days");
        run(busy, UUID.randomUUID(), "playing", true, 1_000, "2 days");
        var runner = new MapStatsRunner(db, false);
        runner.run(null);
        // The test is one transaction, so the first run is made to have happened a while ago.
        TEST_DB.seed("update map_stats set refreshed_at = now() - interval '10 minutes'");
        var quietAt = stats(quiet).refreshedAt();

        run(busy, UUID.randomUUID(), "playing", false, 1_000, "0 seconds");
        runner.run(null);

        assertEquals(quietAt, stats(quiet).refreshedAt());
        assertNotEquals(quietAt, stats(busy).refreshedAt());
        assertEquals(2, stats(busy).playCount());
    }

    @Test
    void aRating_refreshesItsMap() {
        var map = map();
        run(map, UUID.randomUUID(), "playing", true, 1_000, "2 days");
        var runner = new MapStatsRunner(db, false);
        runner.run(null);

        TEST_DB.seed(
            "insert into map_ratings (map_id, player_id, rating) values ('%s', '%s', 'liked')".formatted(
                map,
                UUID.randomUUID()
            )
        );
        runner.run(null);

        assertEquals(1, stats(map).likes());

        TEST_DB.seed(
            "update map_ratings set rating = null, rated_at = now() where map_id = '" + map + "'"
        );
        runner.run(null);

        assertEquals(0, stats(map).likes());
    }

    @Test
    void aMapWithNoVerifyTime_trustsItsPlayers() {
        var map = map();
        run(map, UUID.randomUUID(), "playing", true, 40_000, "1 day");

        new MapStatsRunner(db, false).run(null);

        assertEquals(40_000, stats(map).estLengthMs());
    }
}
