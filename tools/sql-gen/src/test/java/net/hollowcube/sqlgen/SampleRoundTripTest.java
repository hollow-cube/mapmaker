package net.hollowcube.sqlgen;

import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import sample.db.Gadget;
import sample.db.GadgetReview;
import sample.db.GadgetStatus;
import sample.db.GadgetsQueries.InsertGadgetParams;
import sample.db.GadgetsQueries.ListGadgetsWithReviewRow;
import sample.db.GadgetsQueries.ListGadgetsWithStarsRow;
import sample.db.GadgetsQueries.PairGadgetsRow;
import sample.db.SampleDatabase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Runs the sample corpus's generated code against a real Postgres, once per mapped type.
///
/// The golden test only proves the emitters are stable. This proves the code they emit reads and
/// writes what the server actually stores — the part that quietly breaks when a type mapping is a
/// plausible guess rather than a correct one.
class SampleRoundTripTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
    // Postgres keeps timestamps to microseconds, so anything finer would not survive the round trip.
    private static final Instant CREATED_AT = Instant.parse("2026-08-25T12:34:56.123456Z").truncatedTo(ChronoUnit.MICROS);

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("sample/db/migrations");

    private final SampleDatabase db = TEST_DB.database(SampleDatabase::new);

    private static InsertGadgetParams gadget(UUID id, String label) {
        return new InsertGadgetParams(id, GadgetStatus.ACTIVE, label, 7, 1.5,
            new BigDecimal("19.99"), new byte[]{1, 2, 3}, "{\"kind\": \"demo\", \"level\": 3}",
            CREATED_AT, LocalDate.of(2026, 8, 25), List.of("alpha", "beta"), List.of(3, 1, 4));
    }

    @Test
    void everyMappedTypeSurvivesTheRoundTrip() {
        assertEquals(1, db.gadgets.insertGadget(gadget(ID, "widget one")));

        Gadget read = db.gadgets.getGadget(ID);
        assertNotNull(read);
        assertEquals(ID, read.id());
        assertEquals(GadgetStatus.ACTIVE, read.status());
        assertEquals("widget one", read.label());
        assertEquals(7, read.quantity());
        assertEquals(1.5, read.weight());
        assertEquals(0, new BigDecimal("19.99").compareTo(read.price()));
        assertArrayEquals(new byte[]{1, 2, 3}, read.payload());
        assertTrue(read.metadata().contains("\"kind\"") && read.metadata().contains("\"level\""), read.metadata());
        assertEquals(CREATED_AT, read.createdAt());
        assertEquals(LocalDate.of(2026, 8, 25), read.releasedOn());
        assertEquals(List.of("alpha", "beta"), read.tags());
        assertEquals(List.of(3, 1, 4), read.ranks());
    }

    @Test
    void nullableColumnsComeBackNull() {
        db.gadgets.insertGadget(gadget(ID, "widget one"));

        Gadget read = db.gadgets.getGadget(ID);
        assertNotNull(read);
        assertNull(read.note());
        assertNull(read.retiredAt());
    }

    @Test
    void oneReturnsNullWhenNothingMatches() {
        assertNull(db.gadgets.getGadget(UUID.randomUUID()));
    }

    @Test
    void singleColumnResultsUnwrapToTheScalar() {
        db.gadgets.insertGadget(gadget(ID, "widget one"));
        db.gadgets.insertGadget(gadget(UUID.randomUUID(), "widget two"));

        assertEquals(2L, db.gadgets.countGadgets());
        assertEquals(List.of("widget one", "widget two"), db.gadgets.listGadgetLabels());
    }

    @Test
    void aRepeatedPlaceholderBindsTheSameArgumentTwice() {
        db.gadgets.insertGadget(gadget(ID, "needle"));
        var other = UUID.randomUUID();
        db.gadgets.insertGadget(gadget(other, "haystack"));
        TEST_DB.seed("update gadget set note = 'needle' where id = '" + other + "'");

        assertEquals(2, db.gadgets.findGadgetsByText("needle").size());
        assertEquals(0, db.gadgets.findGadgetsByText("nothing").size());
    }

    @Test
    void execReturnsTheUpdateCount() {
        db.gadgets.insertGadget(gadget(ID, "widget one"));

        assertEquals(1, db.gadgets.deleteGadget(ID));
        assertEquals(0, db.gadgets.deleteGadget(ID));
    }

    @Test
    void returningFeedsAnOneQuery() {
        db.gadgets.insertGadget(gadget(ID, "widget one"));

        GadgetReview review = db.gadgets.insertReview(1, ID, 5);
        assertNotNull(review);
        assertEquals(1, review.id());
        assertEquals(ID, review.gadgetId());
        assertEquals(5, review.stars());
    }

    @Test
    void nullableDirectiveWidensAnOuterJoinColumn() {
        db.gadgets.insertGadget(gadget(ID, "widget one"));

        ListGadgetsWithStarsRow unrated = db.gadgets.listGadgetsWithStars().getFirst();
        assertNull(unrated.stars(), "a left join with no match has to come back null");

        db.gadgets.insertReview(1, ID, 4);
        assertEquals(4, db.gadgets.listGadgetsWithStars().getFirst().stars());
    }

    @Test
    void nullableDirectiveWidensAnEmbeddedTable() {
        db.gadgets.insertGadget(gadget(ID, "widget one"));

        ListGadgetsWithReviewRow unrated = db.gadgets.listGadgetsWithReview().getFirst();
        assertEquals(ID, unrated.gadget().id());
        assertNull(unrated.review());

        db.gadgets.insertReview(1, ID, 4);
        ListGadgetsWithReviewRow rated = db.gadgets.listGadgetsWithReview().getFirst();
        assertNotNull(rated.review());
        assertEquals(4, rated.review().stars());
    }

    @Test
    void selfJoinEmbedsAreNamedByAlias() {
        var other = UUID.fromString("22222222-2222-3333-4444-555555555555");
        db.gadgets.insertGadget(gadget(ID, "twin"));
        db.gadgets.insertGadget(gadget(other, "twin"));

        List<PairGadgetsRow> pairs = db.gadgets.pairGadgets();
        assertEquals(1, pairs.size());
        assertEquals(ID, pairs.getFirst().a().id());
        assertEquals(other, pairs.getFirst().b().id());
        assertEquals("twin", pairs.getFirst().b().label());
    }
}
