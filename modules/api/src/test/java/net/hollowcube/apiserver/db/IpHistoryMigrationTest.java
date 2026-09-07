package net.hollowcube.apiserver.db;

import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IpHistoryMigrationTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("src/main/sql/migrations");

    @Test
    void convertsLegacyUtcValuesRegardlessOfSessionTimeZone() throws Exception {
        TEST_DB.seed("""
            alter table ip_history
                alter column first_seen type timestamp using first_seen at time zone 'UTC',
                alter column last_seen type timestamp using last_seen at time zone 'UTC';
            insert into player_data (id, username, first_join, last_online, online) values ('11111111-1111-1111-1111-111111111111', 'p', now(), now(), false);
            insert into ip_history (player_id, address, first_seen, last_seen, seen_count)
            values ('11111111-1111-1111-1111-111111111111', '127.0.0.1', '2026-01-15 12:34:56.123456', '2026-07-15 01:02:03.654321', 2);
            set local time zone '-05:00';
            """);
        TEST_DB.seed(Files.readString(Path.of("src/main/sql/migrations/0020_ip_history_timestamptz.sql")));

        try (var st = TEST_DB.conn().createStatement();
             var rs = st.executeQuery("select ip_history.* from ip_history")) {
            assertTrue(rs.next());
            var row = IpHistory.read(rs, 1);
            assertEquals(Instant.parse("2026-01-15T12:34:56.123456Z"), row.firstSeen());
            assertEquals(Instant.parse("2026-07-15T01:02:03.654321Z"), row.lastSeen());
            assertEquals(2, row.seenCount());
            assertFalse(rs.next());
        }
    }
}
