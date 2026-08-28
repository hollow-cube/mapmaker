package net.hollowcube.sqlgen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Comparator;
import java.util.List;

/// The throwaway Postgres every query is described against: booted empty, migrated, thrown away.
///
/// pglite4j runs a real Postgres 17 on a WASM runtime in this process, so this needs no daemon and
/// no container. It is also the one swap point if that ever stops being true — everything downstream
/// only sees a [Connection].
final class AnalysisDb implements AutoCloseable {

    /// pglite4j keys its in-memory databases on the URL's host and keeps every one it has ever been
    /// asked for alive for the life of the JVM. Booting a fresh name per run would leak a whole
    /// Postgres each time, so there is one instance and each boot resets it instead. Only one
    /// [AnalysisDb] can be open at a time as a result, which is all the generator ever needs.
    private static final String URL = "jdbc:pglite:memory://sqlgen-analysis";

    private final Connection conn;

    private AnalysisDb(Connection conn) {
        this.conn = conn;
    }

    static AnalysisDb boot(Path migrations) {
        try {
            var conn = DriverManager.getConnection(URL);
            try {
                try (Statement st = conn.createStatement()) {
                    st.execute("drop schema if exists public cascade");
                    st.execute("create schema public");
                }
                applyMigrations(conn, migrationFiles(migrations));
            } catch (Throwable t) {
                conn.close();
                throw t;
            }
            return new AnalysisDb(conn);
        } catch (SQLException e) {
            throw new GenException("failed to boot the analysis database: " + e.getMessage(), e);
        }
    }

    /// Every `.sql` file in the directory, in filename order. There are no down migrations and no
    /// version table — a migration is just DDL that has to run before the one after it.
    static List<Path> migrationFiles(Path dir) {
        if (!Files.isDirectory(dir)) throw new GenException("no migrations directory at " + dir);
        try (var files = Files.list(dir)) {
            return files.filter(path -> path.getFileName().toString().endsWith(".sql"))
                .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static void applyMigrations(Connection conn, List<Path> files) throws SQLException {
        try (Statement st = conn.createStatement()) {
            // pglite4j starts sessions with search_path at pg_catalog, so unqualified DDL would
            // land in the system schema and then be invisible to every catalog query we run.
            st.execute("set search_path to public");
            for (var file : files) {
                try {
                    st.execute(Files.readString(file));
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                } catch (SQLException e) {
                    throw new GenException("migration " + file.getFileName() + " failed: " + e.getMessage(), e);
                }
            }
        }
    }

    Connection conn() {
        return conn;
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException ignored) {
            // Nothing downstream can act on a failure to close a database we are discarding.
        }
    }
}
