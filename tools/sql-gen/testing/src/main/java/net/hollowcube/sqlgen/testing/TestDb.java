package net.hollowcube.sqlgen.testing;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/// A real Postgres for a test class, built from the same migrations the generator was pointed at.
///
/// ```java
/// @RegisterExtension
/// static final TestDb TEST_DB = TestDb.of("src/main/sql/migrations");
///
/// @Test void search() {
///     var db = TEST_DB.database(ApiDatabase::new);
///     TEST_DB.seed("insert into head_db values (1, 'mob', 'Creeper', '{}', 'tex')");
///     assertEquals(1, db.heads.getRandomHeads(10).size());
/// }
/// ```
///
/// The database boots once per JVM per migration directory — roughly a second — and each test then
/// gets a clean one for milliseconds. The engine is single-user, so keep tests that use this out of
/// parallel execution.
public final class TestDb implements BeforeAllCallback, BeforeEachCallback, AfterEachCallback {

    /// How a test's writes get undone before the next one.
    public enum Mode {
        /// Wrap each test in a transaction and roll it back. Milliseconds per test, but the database
        /// is already inside a transaction, so a generated `db.tx(...)` throws rather than committing
        /// past the rollback.
        ROLLBACK,
        /// Let each test commit for real, and truncate every table afterwards. Slower, and the only
        /// way to exercise `db.tx(...)` itself.
        TRUNCATE
    }

    private static final Map<Path, Instance> INSTANCES = new ConcurrentHashMap<>();

    private final Path migrations;
    private final Mode mode;
    private Instance instance;

    private TestDb(Path migrations, Mode mode) {
        this.migrations = migrations.toAbsolutePath().normalize();
        this.mode = mode;
    }

    /// A rollback-mode database from the migrations at `migrations`, resolved against the working
    /// directory (which for a Gradle test is the project directory).
    public static TestDb of(String migrations) {
        return of(Path.of(migrations), Mode.ROLLBACK);
    }

    public static TestDb of(String migrations, Mode mode) {
        return of(Path.of(migrations), mode);
    }

    public static TestDb of(Path migrations, Mode mode) {
        return new TestDb(migrations, mode);
    }

    /// The connection every query in this test runs on, for seeding and for anything with no
    /// generated query behind it.
    public Connection conn() {
        if (instance == null) throw new IllegalStateException("TestDb has not started; register it with @RegisterExtension");
        return instance.conn;
    }

    /// Runs `sql` directly. Multiple statements separated by `;` are fine.
    public void seed(String sql) {
        try (Statement st = conn().createStatement()) {
            st.execute(sql);
        } catch (SQLException e) {
            throw new IllegalStateException("failed to seed: " + e.getMessage(), e);
        }
    }

    /// Builds a generated database over this test's connection: `TEST_DB.database(ApiDatabase::new)`.
    public <D> D database(Function<DataSource, D> constructor) {
        return constructor.apply(dataSource());
    }

    public DataSource dataSource() {
        var refusal = mode == Mode.ROLLBACK
            ? "this test runs in TestDb.Mode.ROLLBACK, which already holds a transaction open; use "
                + "Mode.TRUNCATE to test code that commits"
            : null;
        return new SingleConnectionDataSource(conn(), refusal);
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        instance = INSTANCES.computeIfAbsent(migrations, TestDb::boot);
    }

    @Override
    public void beforeEach(ExtensionContext context) throws SQLException {
        if (mode == Mode.ROLLBACK) conn().setAutoCommit(false);
    }

    @Override
    public void afterEach(ExtensionContext context) throws SQLException {
        if (mode == Mode.ROLLBACK) {
            conn().rollback();
            conn().setAutoCommit(true);
            return;
        }
        if (instance.tables.isEmpty()) return;
        try (Statement st = conn().createStatement()) {
            st.execute("truncate " + String.join(", ", instance.tables) + " restart identity cascade");
        }
    }

    private record Instance(Connection conn, List<String> tables) {
    }

    private static Instance boot(Path migrations) {
        if (!Files.isDirectory(migrations)) throw new IllegalStateException("no migrations directory at " + migrations);
        try {
            // pglite4j keys its in-memory databases on the URL host and keeps each alive for the
            // life of the JVM, so one name per migration set gives one boot per set and no leak.
            var conn = DriverManager.getConnection("jdbc:pglite:memory://testdb-"
                + Integer.toHexString(migrations.toString().hashCode()));
            try (Statement st = conn.createStatement()) {
                // pglite4j sessions start with search_path at pg_catalog, so unqualified DDL would
                // land in the system schema.
                st.execute("set search_path to public");
                for (var file : sqlFiles(migrations)) st.execute(Files.readString(file));
            }
            return new Instance(conn, userTables(conn));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (SQLException e) {
            throw new IllegalStateException("failed to build the test database: " + e.getMessage(), e);
        }
    }

    private static List<Path> sqlFiles(Path dir) throws IOException {
        try (var files = Files.list(dir)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".sql"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
        }
    }

    private static List<String> userTables(Connection conn) throws SQLException {
        var tables = new ArrayList<String>();
        try (Statement st = conn.createStatement();
             var rs = st.executeQuery("""
                 select c.relname
                 from pg_class c join pg_namespace n on n.oid = c.relnamespace
                 where n.nspname = 'public' and c.relkind = 'r'
                 order by c.relname""")) {
            while (rs.next()) tables.add(rs.getString(1));
        }
        return List.copyOf(tables);
    }
}
