package net.hollowcube.sqlgen;

import net.hollowcube.sqlgen.runtime.SqlFragment;
import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import sample.db.GadgetStatus;
import sample.db.GadgetsQueries.InsertGadgetParams;
import sample.db.SampleDatabase;
import sample.db.GadgetsQueries.SearchGadgetsRow;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

/// The `/* where */` and `/* order by */` holes: the base query still runs with nothing filled in,
/// each fill lands in the right clause, and a fragment's own parameters bind in the right slots.
class SqlFragmentTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("sample/db/migrations");

    private final SampleDatabase db = TEST_DB.database(SampleDatabase::new);

    @BeforeEach
    void seed() {
        insert("aaaaaaaa-0000-0000-0000-000000000001", "alpha", 1);
        insert("aaaaaaaa-0000-0000-0000-000000000002", "bravo", 5);
        insert("aaaaaaaa-0000-0000-0000-000000000003", "charlie", 3);
    }

    private void insert(String id, String label, int quantity) {
        db.gadgets.insertGadget(new InsertGadgetParams(UUID.fromString(id), GadgetStatus.ACTIVE, label,
            quantity, 1.0, BigDecimal.ONE, new byte[0], "{}", Instant.EPOCH, LocalDate.EPOCH,
            List.of(), List.of()));
    }

    private static List<String> labels(List<SearchGadgetsRow> rows) {
        return rows.stream().map(row -> row.gadget().label()).toList();
    }

    @Test
    void bothHolesEmptyRunsTheBaseQuery() {
        var rows = db.gadgets.searchGadgets(2, 10, 0, null, null);

        assertEquals(3, rows.size());
        assertEquals(3, rows.getFirst().totalCount());
    }

    @Test
    void aWhereFragmentNarrowsTheResult() {
        var rows = db.gadgets.searchGadgets(2, 10, 0, SqlFragment.of("quantity > ?", 2), null);

        assertEquals(2, rows.size());
        assertEquals(2, rows.getFirst().totalCount(), "the window count sees the fragment too");
    }

    @Test
    void anOrderByFragmentReordersTheResult() {
        var ascending = db.gadgets.searchGadgets(2, 10, 0, null, SqlFragment.of("quantity asc"));
        var descending = db.gadgets.searchGadgets(2, 10, 0, null, SqlFragment.of("quantity desc"));

        assertEquals(List.of("alpha", "charlie", "bravo"), labels(ascending));
        assertEquals(List.of("bravo", "charlie", "alpha"), labels(descending));
    }

    @Test
    void bothHolesFilledBindInStatementOrder() {
        var rows = db.gadgets.searchGadgets(2, 10, 0,
            SqlFragment.of("quantity between ? and ?", 1, 4),
            SqlFragment.of("label desc"));

        assertEquals(List.of("charlie", "alpha"), labels(rows));
    }

    @Test
    void theHoleParametersSitBetweenTheGeneratedOnes() {
        // prefixLength binds before the fragment, limit and offset after it: if the splice were off
        // by one, the fragment's argument would land in limit and this would come back wrong.
        var rows = db.gadgets.searchGadgets(3, 1, 1,
            SqlFragment.of("label <> ?", "bravo"), SqlFragment.of("label asc"));

        assertEquals(List.of("charlie"), labels(rows));
        assertEquals(2, rows.getFirst().totalCount());
        assertEquals("cha", rows.getFirst().labelPrefix());
    }

    @Test
    void aFragmentThatMatchesNothingIsStillAValidQuery() {
        var rows = db.gadgets.searchGadgets(2, 10, 0, SqlFragment.of("label = ?", "nothing"), null);

        assertEquals(List.of(), labels(rows));
    }
}
