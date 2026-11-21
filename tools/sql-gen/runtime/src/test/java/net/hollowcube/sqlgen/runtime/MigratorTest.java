package net.hollowcube.sqlgen.runtime;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MigratorTest {

    @TempDir
    Path dir;

    // pglite4j keeps every in-memory database alive for the life of the JVM, so the tests share one
    // and start from an empty public schema rather than booting their own.
    private static Connection conn;

    @BeforeAll
    static void open() throws SQLException {
        conn = DriverManager.getConnection("jdbc:pglite:memory://migrator-test");
    }

    @AfterAll
    static void close() throws SQLException {
        conn.close();
    }

    @BeforeEach
    void reset() throws SQLException {
        try (var st = conn.createStatement()) {
            st.execute("drop schema if exists public cascade");
            st.execute("create schema public");
            st.execute("set search_path to public");
        }
    }

    @Test
    void appliesPendingInVersionOrderOnce() throws Exception {
        write("0002_add_column.sql", "alter table thing add column b int;");
        write("0001_create.sql", "create table thing (a int);");

        assertEquals(List.of(1, 2), versions(Migrator.migrate(conn, dir)));
        assertEquals(List.of("a", "b"), columns("thing"));
        assertEquals(List.of(), Migrator.migrate(conn, dir));
    }

    @Test
    void breakpointsSplitStatements() throws Exception {
        write("0001_create.sql", """
            create table thing (a int);
            --> statement-breakpoint
            create table other (b int);
            """);

        assertEquals(2, Migrator.statements(Files.readString(dir.resolve("0001_create.sql"))).size());
        Migrator.migrate(conn, dir);
        assertEquals(List.of("b"), columns("other"));
    }

    @Test
    void failedMigrationLeavesNoTrace() throws Exception {
        write("0001_create.sql", "create table thing (a int);");
        write("0002_broken.sql", "create table other (a int); select * from missing;");

        var error = assertThrows(SQLException.class, () -> Migrator.migrate(conn, dir));
        assertTrue(error.getMessage().contains("0002_broken.sql"), error.getMessage());
        assertEquals(List.of(), columns("other"));
        assertEquals(List.of(1), recorded());
    }

    @Test
    void changedAppliedFileIsRefused() throws Exception {
        write("0001_create.sql", "create table thing (a int);");
        Migrator.migrate(conn, dir);
        write("0001_create.sql", "create table thing (a bigint);");

        assertThrows(IllegalStateException.class, () -> Migrator.migrate(conn, dir));
    }

    @Test
    void renamedAppliedFileIsRefused() throws Exception {
        write("0001_create.sql", "create table thing (a int);");
        Migrator.migrate(conn, dir);
        Files.move(dir.resolve("0001_create.sql"), dir.resolve("0001_make.sql"));

        assertThrows(IllegalStateException.class, () -> Migrator.migrate(conn, dir));
    }

    @Test
    void pendingFileOlderThanAppliedIsRefused() throws Exception {
        write("0002_create.sql", "create table thing (a int);");
        Migrator.migrate(conn, dir);
        write("0001_late.sql", "create table late (a int);");

        assertThrows(IllegalStateException.class, () -> Migrator.migrate(conn, dir));
    }

    @Test
    void baselineRecordsWithoutRunning() throws Exception {
        write("0001_create.sql", "create table thing (a int);");
        write("0002_create_other.sql", "create table other (a int);");

        assertEquals(List.of(1), versions(Migrator.baseline(conn, dir, 1)));
        assertEquals(List.of(), columns("thing"));
        assertEquals(List.of(2), versions(Migrator.migrate(conn, dir)));
        assertEquals(List.of("a"), columns("other"));
    }

    private void write(String name, String sql) throws IOException {
        Files.writeString(dir.resolve(name), sql);
    }

    private static List<Integer> versions(List<Migrator.Migration> migrations) {
        return migrations.stream().map(Migrator.Migration::version).toList();
    }

    private List<Integer> recorded() throws SQLException {
        var versions = new ArrayList<Integer>();
        try (var st = conn.createStatement();
             var rs = st.executeQuery("select version from schema_migrations order by version")) {
            while (rs.next()) versions.add(rs.getInt(1));
        }
        return versions;
    }

    private List<String> columns(String table) throws SQLException {
        var columns = new ArrayList<String>();
        try (var ps = conn.prepareStatement("""
            select a.attname
            from pg_attribute a
                     join pg_class c on c.oid = a.attrelid
                     join pg_namespace n on n.oid = c.relnamespace
            where n.nspname = 'public' and c.relname = ? and a.attnum > 0 and not a.attisdropped
            order by a.attnum""")) {
            ps.setString(1, table);
            try (var rs = ps.executeQuery()) {
                while (rs.next()) columns.add(rs.getString(1));
            }
        }
        return columns;
    }
}
