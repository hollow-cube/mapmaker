package net.hollowcube.apiserver.job;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CronTest {

    private static Instant next(String cron, String from) {
        return Cron.parse(cron).next(Instant.parse(from));
    }

    @Test
    void everyFiveMinutes_isTheNextAlignedBoundary() {
        assertEquals(Instant.parse("2026-08-29T12:05:00Z"), next("*/5 * * * *", "2026-08-29T12:03:17Z"));
        // Exactly on a boundary means the next one, so a run cannot fire twice for one boundary.
        assertEquals(Instant.parse("2026-08-29T12:10:00Z"), next("*/5 * * * *", "2026-08-29T12:05:00Z"));
        assertEquals(Instant.parse("2026-08-29T12:05:00Z"), next("*/5 * * * *", "2026-08-29T12:04:59.999Z"));
        assertEquals(Instant.parse("2026-08-30T00:00:00Z"), next("*/5 * * * *", "2026-08-29T23:57:00Z"));
    }

    @Test
    void jumpsWholeFieldsRatherThanWalking() {
        assertEquals(Instant.parse("2026-08-30T04:00:00Z"), next("0 4 * * *", "2026-08-29T05:00:00Z"));
        assertEquals(Instant.parse("2026-08-29T04:30:00Z"), next("30 4 * * *", "2026-08-29T04:00:00Z"));
        assertEquals(Instant.parse("2026-09-01T00:00:00Z"), next("0 0 1 * *", "2026-08-15T00:00:00Z"));
        assertEquals(Instant.parse("2027-01-01T00:00:00Z"), next("0 0 1 1 *", "2026-08-15T00:00:00Z"));
        assertEquals(Instant.parse("2028-02-29T00:00:00Z"), next("0 0 29 2 *", "2026-03-01T00:00:00Z"));
        assertEquals(Instant.parse("2026-08-29T23:45:00Z"), next("15,45 * * * *", "2026-08-29T23:20:00Z"));
        assertEquals(Instant.parse("2026-08-29T09:00:00Z"), next("0 9-17 * * *", "2026-08-29T03:00:00Z"));
    }

    @Test
    void weekdays_countFromSundayAndSevenIsSundayToo() {
        // 2026-08-29 is a Saturday.
        assertEquals(Instant.parse("2026-08-31T00:00:00Z"), next("0 0 * * 1", "2026-08-29T00:00:00Z"));
        assertEquals(Instant.parse("2026-08-30T00:00:00Z"), next("0 0 * * 0", "2026-08-29T00:00:00Z"));
        assertEquals(Instant.parse("2026-08-30T00:00:00Z"), next("0 0 * * 7", "2026-08-29T00:00:00Z"));
        assertEquals(Instant.parse("2026-08-31T00:00:00Z"), next("0 0 * * 1-5", "2026-08-29T00:00:00Z"));
    }

    @Test
    void bothDayFieldsRestricted_matchesEither() {
        // The 15th is a Tuesday; Monday the 14th comes first.
        assertEquals(Instant.parse("2026-09-14T00:00:00Z"), next("0 0 15 * 1", "2026-09-10T00:00:00Z"));
        assertEquals(Instant.parse("2026-09-15T00:00:00Z"), next("0 0 15 * 1", "2026-09-14T00:00:00Z"));
    }

    @Test
    void rejectsWhatItCannotMean() {
        assertThrows(IllegalArgumentException.class, () -> Cron.parse("* * * *"));
        assertThrows(IllegalArgumentException.class, () -> Cron.parse("60 * * * *"));
        assertThrows(IllegalArgumentException.class, () -> Cron.parse("0 0 0 * *"));
        assertThrows(IllegalArgumentException.class, () -> Cron.parse("5-1 * * * *"));
        assertThrows(IllegalArgumentException.class, () -> Cron.parse("every 5 * * *"));
        assertThrows(IllegalArgumentException.class, () -> Cron.parse("0 0 31 2 *"), "never matches");
    }
}
