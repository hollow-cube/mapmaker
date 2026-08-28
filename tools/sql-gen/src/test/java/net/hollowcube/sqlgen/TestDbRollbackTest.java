package net.hollowcube.sqlgen;

import net.hollowcube.sqlgen.testing.TestDb;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.extension.RegisterExtension;
import sample.db.SampleDatabase;
import sample.db.Widget;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// [TestDb] in its default mode, used the way a consumer would.
///
/// The two ordered tests are the point: the first writes, the second has to find none of it.
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TestDbRollbackTest {

    @RegisterExtension
    static final TestDb TEST_DB = TestDb.of("sample/db/migrations");

    private final SampleDatabase db = TEST_DB.database(SampleDatabase::new);

    @Test
    @Order(1)
    void writesAreVisibleWithinTheTest() {
        TEST_DB.seed("insert into widget values (1, 'first', array['a'], null)");

        List<Widget> widgets = db.widgets.listWidgets(10);
        assertEquals(1, widgets.size());
        assertEquals("first", widgets.getFirst().name());
    }

    @Test
    @Order(2)
    void thePreviousTestsWritesAreGone() {
        assertEquals(List.of(), db.widgets.listWidgets(10));
    }

    @Test
    @Order(3)
    void aTransactionIsRefusedBecauseItWouldCommitPastTheRollback() {
        var failure = assertThrows(IllegalStateException.class, () -> db.tx(tx -> {
        }));

        assertTrue(failure.getMessage().contains("Mode.TRUNCATE"), failure.getMessage());
    }

    @Test
    @Order(4)
    void theConnectionIsReachableForRawStatements() throws Exception {
        try (var st = TEST_DB.conn().createStatement(); var rs = st.executeQuery("select 1")) {
            assertTrue(rs.next());
            assertEquals(1, rs.getInt(1));
        }
    }
}
