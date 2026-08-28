package net.hollowcube.sqlgen;

import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import sample.db.GadgetStatus;
import sample.db.GadgetsQueries.InsertGadgetParams;
import sample.db.SampleDatabase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// [TestDb] in truncate mode, which is the mode that exists so `db.tx(...)` can be tested at all.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestDbTruncateTest {

    private static final UUID ID = UUID.fromString("cccccccc-0000-0000-0000-000000000001");

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("sample/db/migrations", TestDb.Mode.TRUNCATE);

    private final SampleDatabase db = TEST_DB.database(SampleDatabase::new);

    private static InsertGadgetParams gadget(String label) {
        return new InsertGadgetParams(ID, GadgetStatus.DRAFT, label, 1, 1.0, BigDecimal.ONE,
            new byte[0], "{}", Instant.EPOCH, LocalDate.EPOCH, List.of(), List.of());
    }

    @Test
    @Order(1)
    void aTransactionCommitsForReal() {
        db.tx(tx -> tx.gadgets.insertGadget(gadget("committed")));

        assertNotNull(db.gadgets.getGadget(ID));
    }

    @Test
    @Order(2)
    void theCommittedRowIsTruncatedAwayBeforeTheNextTest() {
        assertEquals(0L, db.gadgets.countGadgets());
    }

    @Test
    @Order(3)
    void aFailedTransactionStillRollsBack() {
        assertThrows(IllegalStateException.class, () -> db.tx(tx -> {
            tx.gadgets.insertGadget(gadget("doomed"));
            throw new IllegalStateException("nope");
        }));

        assertNull(db.gadgets.getGadget(ID));
    }
}
