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
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// The parts of a generated database that are not a query: transactions, the fake builder, and the
/// escape hatches out to raw JDBC.
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
    void aTransactionCommitsWhenTheBlockReturns() {
        db.tx(tx -> {
            tx.gadgets.insertGadget(gadget(ID, "one"));
            tx.gadgets.insertReview(1, ID, 5);
        });

        assertNotNull(db.gadgets.getGadget(ID));
        assertEquals(1L, db.gadgets.countGadgets());
    }

    @Test
    void aTransactionRollsBackEverythingWhenTheBlockThrows() {
        var failure = assertThrows(IllegalStateException.class, () -> db.tx(tx -> {
            tx.gadgets.insertGadget(gadget(ID, "one"));
            throw new IllegalStateException("nope");
        }));

        assertEquals("nope", failure.getMessage());
        assertNull(db.gadgets.getGadget(ID), "the insert should not have survived the rollback");
    }

    @Test
    void txResultHandsBackTheBlocksValue() {
        Gadget written = db.txResult(tx -> {
            tx.gadgets.insertGadget(gadget(ID, "one"));
            return tx.gadgets.getGadget(ID);
        });

        assertNotNull(written);
        assertEquals("one", written.label());
    }

    @Test
    void aTransactionSeesItsOwnUncommittedWrites() {
        db.tx(tx -> {
            tx.gadgets.insertGadget(gadget(ID, "one"));
            assertNotNull(tx.gadgets.getGadget(ID));
        });
    }

    @Test
    void nestingATransactionIsRefused() {
        var failure = assertThrows(IllegalStateException.class, () -> db.tx(outer -> db.tx(inner -> {
        })));

        assertTrue(failure.getMessage().contains("nested transactions"), failure.getMessage());
    }

    @Test
    void aFailedTransactionLeavesTheConnectionUsable() {
        assertThrows(IllegalStateException.class, () -> db.tx(tx -> {
            throw new IllegalStateException("nope");
        }));

        db.gadgets.insertGadget(gadget(ID, "one"));
        assertNotNull(db.gadgets.getGadget(ID));
    }

    @Test
    void txExposesTheConnectionForStatementsWithNoGeneratedQuery() {
        db.tx(tx -> {
            tx.gadgets.insertGadget(gadget(ID, "one"));
            try (Statement st = tx.conn().createStatement()) {
                st.execute("update gadget set note = 'by hand'");
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        });

        Gadget read = db.gadgets.getGadget(ID);
        assertNotNull(read);
        assertEquals("by hand", read.note());
    }

    @Test
    void aFakeUsesTheGroupsItWasGiven() {
        var gadgets = new GadgetsQueries.Stub() {
            @Override
            public long countGadgets() {
                return 42L;
            }
        };
        SampleDatabase fake = SampleDatabase.fake().gadgets(gadgets).build();

        assertEquals(42L, fake.gadgets.countGadgets());
        assertSame(gadgets, fake.gadgets);
    }

    @Test
    void aFakeThrowsForTheQueriesItWasNotGiven() {
        SampleDatabase fake = SampleDatabase.fake().build();

        var failure = assertThrows(UnsupportedOperationException.class, fake.gadgets::countGadgets);
        assertTrue(failure.getMessage().contains("countGadgets"), failure.getMessage());
    }

    @Test
    void aFakeHasNoDataSourceToHandOut() {
        SampleDatabase fake = SampleDatabase.fake().build();

        var failure = assertThrows(IllegalStateException.class, fake::dataSource);
        assertTrue(failure.getMessage().contains("fake"), failure.getMessage());
    }

    @Test
    void theDataSourceIsReachableForThingsTheGeneratorDoesNotCover() throws Exception {
        try (var st = db.dataSource().getConnection().createStatement()) {
            assertTrue(st.execute("select 1"));
        }
    }
}
