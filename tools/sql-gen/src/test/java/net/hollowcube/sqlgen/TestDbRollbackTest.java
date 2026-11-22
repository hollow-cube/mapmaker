package net.hollowcube.sqlgen;

import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import sample.db.SampleDatabase;
import sample.db.Widget;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// [TestDb] in its default mode, used the way a consumer would. The first test writes, the second
/// has to find none of it.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestDbRollbackTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("sample/db/migrations");

    private final SampleDatabase db = TEST_DB.database(SampleDatabase::new);

    @Test
    @Order(1)
    void aTransactionCommitsOrUndoesOnlyItselfInsideTheTest() {
        TEST_DB.seed("insert into widget values (1, 'seeded', array['a'], null)");
        db.tx(tx -> insert(tx.conn(), 2, "committed"));
        assertThrows(IllegalStateException.class, () -> db.tx(tx -> {
            insert(tx.conn(), 3, "rolled back");
            throw new IllegalStateException("boom");
        }));

        assertEquals(
            List.of("seeded", "committed"),
            db.widgets.listWidgets(10).stream().map(Widget::name).toList()
        );
    }

    @Test
    @Order(2)
    void thePreviousTestsWritesAreGone() {
        assertEquals(List.of(), db.widgets.listWidgets(10));
    }

    private static void insert(Connection conn, int id, String name) {
        try (var st = conn.createStatement()) {
            st.execute("insert into widget values (" + id + ", '" + name + "', array['a'], null)");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
