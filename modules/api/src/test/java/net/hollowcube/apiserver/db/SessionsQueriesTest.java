package net.hollowcube.apiserver.db;

import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// Drives the generated session queries against a Postgres built from the copy of Go's schema
/// they were described against.
class SessionsQueriesTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("src/main/sql/migrations");

    private final ApiDatabase db = TEST_DB.database(ApiDatabase::new);

    @Test
    void countPlayerSessions_countsEveryRowHiddenOrNot() {
        assertEquals(0, db.sessions.countPlayerSessions());

        TEST_DB.seed("""
            insert into player_sessions (player_id, proxy_id, skin_texture, skin_signature, hidden) values
                ('6a7b0f9e-0000-4000-8000-000000000001', 'proxy-1', 'tex', 'sig', false),
                ('6a7b0f9e-0000-4000-8000-000000000002', 'proxy-1', 'tex', 'sig', true)""");

        assertEquals(2, db.sessions.countPlayerSessions());
    }
}
