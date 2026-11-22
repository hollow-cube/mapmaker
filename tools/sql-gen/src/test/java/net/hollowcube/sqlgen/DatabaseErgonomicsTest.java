package net.hollowcube.sqlgen;

import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import sample.db.Gadget;
import sample.db.GadgetStatus;
import sample.db.GadgetsQueries;
import sample.db.GadgetsQueries.InsertGadgetParams;
import sample.db.SampleDatabase;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The parts of a generated database that are not a query: transactions and the fake builder.
class DatabaseErgonomicsTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee");

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("sample/db/migrations", TestDb.Mode.TRUNCATE);

    private final SampleDatabase db = TEST_DB.database(SampleDatabase::new);

    private static InsertGadgetParams gadget(UUID id, String label) {
        return new InsertGadgetParams(id, GadgetStatus.DRAFT, label, 1, 1.0, BigDecimal.ONE,
            new byte[0], "{}", Instant.EPOCH, LocalDate.EPOCH, List.of(), List.of());
    }

    @Test
    void aTransactionRollsBackEverythingWhenTheBlockThrows() {
        var failure = assertThrows(IllegalStateException.class, () -> db.tx(tx -> {
            tx.gadgets.insertGadget(gadget(ID, "one"));
            throw new IllegalStateException("nope");
        }));

        assertEquals("nope", failure.getMessage());
        assertNull(db.gadgets.getGadget(ID), "the insert should not have survived the rollback");

        Gadget written = db.txResult(tx -> {
            tx.gadgets.insertGadget(gadget(ID, "two"));
            return tx.gadgets.getGadget(ID);
        });
        assertNotNull(written, "the connection is usable after a failed transaction");
        assertEquals("two", written.label());
    }

    @Test
    void nestingATransactionIsRefused() {
        var failure = assertThrows(IllegalStateException.class, () -> db.tx(outer -> db.tx(inner -> {
        })));

        assertTrue(failure.getMessage().contains("nested transactions"), failure.getMessage());
    }

    @Test
    void afterCommitRunsInOrderAndCanStartANewTransaction() {
        var calls = new ArrayList<String>();
        var completed = db.txResult(tx -> {
            tx.gadgets.insertGadget(gadget(ID, "one"));
            tx.afterCommit(() -> db.tx(next -> {
                assertNotNull(next.gadgets.getGadget(ID));
                calls.add("first");
            }));
            tx.afterCommit(() -> calls.add("second"));
            assertTrue(calls.isEmpty());
            return tx;
        });

        assertEquals(List.of("first", "second"), calls);
        assertThrows(IllegalStateException.class, () -> completed.afterCommit(() -> {}));
    }

    @Test
    void callbackFailureDoesNotUndoCommitOrSkipOtherCallbacksOrReachTheCaller() {
        var calls = new ArrayList<String>();
        db.tx(tx -> {
            tx.gadgets.insertGadget(gadget(ID, "one"));
            tx.afterCommit(() -> { throw new IllegalStateException("first publication failed"); });
            tx.afterCommit(() -> { throw new IllegalStateException("second publication failed"); });
            tx.afterCommit(() -> calls.add("last"));
        });

        assertEquals(List.of("last"), calls);
        assertNotNull(db.gadgets.getGadget(ID));
    }

    @Test
    void aFakeUsesTheGroupsItWasGivenAndThrowsForTheRest() {
        var gadgets = new GadgetsQueries.Stub() {
            @Override
            public long countGadgets() {
                return 42L;
            }
        };
        SampleDatabase fake = SampleDatabase.fake().gadgets(gadgets).build();
        assertEquals(42L, fake.gadgets.countGadgets());
        assertSame(gadgets, fake.gadgets);

        SampleDatabase empty = SampleDatabase.fake().build();
        var failure = assertThrows(UnsupportedOperationException.class, empty.gadgets::countGadgets);
        assertTrue(failure.getMessage().contains("countGadgets"), failure.getMessage());
    }
}
