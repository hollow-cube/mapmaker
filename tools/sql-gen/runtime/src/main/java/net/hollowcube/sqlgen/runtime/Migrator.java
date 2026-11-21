package net.hollowcube.sqlgen.runtime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.regex.Pattern;

/// Brings a database up to a directory of `NNNN_name.sql` migrations, the same directory the
/// generator reads its schema from.
///
/// Each file runs in its own transaction together with the `schema_migrations` row recording it,
/// so a failed file leaves neither its changes nor the row behind. A file that has already been
/// applied must not change or be renamed: its name and checksum are compared on every run, and a
/// pending file numbered below the newest applied one is refused rather than run out of order.
///
/// A file is sent as one query unless it is split by `--> statement-breakpoint` lines, in which
/// case each piece is sent on its own. pglite4j, which sql-gen and the tests run migrations on,
/// treats every socket read as a whole protocol message, so a query that does not arrive in one read
/// intermittently kills its backend (a 9.7KB file did, 5.6KB ones never have); long migrations must
/// be split.
public final class Migrator {

    private static final Logger logger = LoggerFactory.getLogger(Migrator.class);
    private static final Pattern FILE_NAME = Pattern.compile("(\\d+)_(.+)\\.sql");
    private static final Pattern BREAKPOINT = Pattern.compile("(?m)^-->\\s*statement-breakpoint\\s*$");

    /// Session advisory lock key, so two runs against one database queue instead of racing.
    private static final long LOCK_KEY = 0x5c4e_4d16_4a70_0001L;

    public record Migration(int version, String name, Path file, byte[] checksum) {}

    private record Applied(String name, byte[] checksum) {}

    private Migrator() {}

    /// Applies every pending migration in `dir`, in version order, and returns the ones applied.
    public static List<Migration> migrate(Connection conn, Path dir) throws SQLException, IOException {
        return withLock(conn, () -> {
            var applied = new ArrayList<Migration>();
            for (var migration : pending(conn, dir)) {
                apply(conn, migration);
                applied.add(migration);
            }
            return applied;
        });
    }

    /// Records every pending migration up to and including `through` as applied without running
    /// it, for a database whose schema was brought up by hand.
    public static List<Migration> baseline(Connection conn, Path dir, int through) throws SQLException, IOException {
        return withLock(conn, () -> {
            var recorded = new ArrayList<Migration>();
            for (var migration : pending(conn, dir)) {
                if (migration.version() > through) break;
                record(conn, migration);
                recorded.add(migration);
            }
            return recorded;
        });
    }

    public static List<Migration> read(Path dir) throws IOException {
        var byVersion = new TreeMap<Integer, Migration>();
        try (var files = Files.list(dir)) {
            for (var file : files.filter(f -> f.getFileName().toString().endsWith(".sql")).toList()) {
                var match = FILE_NAME.matcher(file.getFileName().toString());
                if (!match.matches())
                    throw new IllegalStateException("migration file is not NNNN_name.sql: " + file.getFileName());
                var migration = new Migration(Integer.parseInt(match.group(1)), match.group(2), file,
                    sha256(Files.readAllBytes(file)));
                var clash = byVersion.put(migration.version(), migration);
                if (clash != null)
                    throw new IllegalStateException("two migrations numbered " + migration.version() + ": "
                        + clash.file().getFileName() + " and " + file.getFileName());
            }
        }
        return List.copyOf(byVersion.values());
    }

    /// The statements `sql` is split into by `--> statement-breakpoint` lines, blank pieces dropped.
    public static List<String> statements(String sql) {
        return BREAKPOINT.splitAsStream(sql).filter(part -> !part.isBlank()).toList();
    }

    private static List<Migration> pending(Connection conn, Path dir) throws SQLException, IOException {
        var applied = applied(conn);
        var files = read(dir);
        var known = new HashMap<Integer, Migration>();
        for (var migration : files) known.put(migration.version(), migration);

        for (var entry : applied.entrySet()) {
            var file = known.get(entry.getKey());
            if (file == null)
                throw new IllegalStateException("migration " + entry.getKey() + " is applied but its file is gone");
            var recorded = entry.getValue();
            if (!file.name().equals(recorded.name()))
                throw new IllegalStateException("migration " + entry.getKey() + " was applied as "
                    + recorded.name() + " but its file is now " + file.file().getFileName());
            if (!Arrays.equals(file.checksum(), recorded.checksum()))
                throw new IllegalStateException("migration " + file.file().getFileName()
                    + " changed after it was applied (checksum " + HexFormat.of().formatHex(recorded.checksum())
                    + ", file now " + HexFormat.of().formatHex(file.checksum()) + ")");
        }

        var newest = applied.isEmpty() ? Integer.MIN_VALUE : applied.lastKey();
        var pending = new ArrayList<Migration>();
        for (var migration : files) {
            if (applied.containsKey(migration.version())) continue;
            if (migration.version() < newest)
                throw new IllegalStateException("migration " + migration.file().getFileName()
                    + " is older than the newest applied migration " + newest);
            pending.add(migration);
        }
        return pending;
    }

    private static NavigableMap<Integer, Applied> applied(Connection conn) throws SQLException {
        try (var st = conn.createStatement()) {
            st.execute("""
                create table if not exists schema_migrations
                (
                    version    integer     not null primary key,
                    name       text        not null,
                    checksum   bytea       not null,
                    applied_at timestamptz not null default now()
                )""");
            var applied = new TreeMap<Integer, Applied>();
            try (var rs = st.executeQuery("select version, name, checksum from schema_migrations")) {
                while (rs.next()) applied.put(rs.getInt(1), new Applied(rs.getString(2), rs.getBytes(3)));
            }
            return applied;
        }
    }

    private static void apply(Connection conn, Migration migration) throws SQLException, IOException {
        var sql = Files.readString(migration.file());
        conn.setAutoCommit(false);
        try {
            try (var st = conn.createStatement()) {
                for (var statement : statements(sql)) st.execute(statement);
            }
            record(conn, migration);
            conn.commit();
            logger.info("applied migration {}", migration.file().getFileName());
        } catch (SQLException e) {
            conn.rollback();
            throw new SQLException("migration " + migration.file().getFileName() + " failed: " + e.getMessage(),
                e.getSQLState(), e);
        } finally {
            conn.setAutoCommit(true);
        }
    }

    private static void record(Connection conn, Migration migration) throws SQLException {
        try (var ps = conn.prepareStatement("insert into schema_migrations (version, name, checksum) values (?, ?, ?)")) {
            ps.setInt(1, migration.version());
            ps.setString(2, migration.name());
            ps.setBytes(3, migration.checksum());
            ps.executeUpdate();
        }
    }

    private interface LockedWork<T> {
        T run() throws SQLException, IOException;
    }

    private static <T> T withLock(Connection conn, LockedWork<T> work) throws SQLException, IOException {
        try (var st = conn.createStatement()) {
            st.execute("select pg_advisory_lock(" + LOCK_KEY + ")");
        }
        try {
            return work.run();
        } finally {
            try (var st = conn.createStatement()) {
                st.execute("select pg_advisory_unlock(" + LOCK_KEY + ")");
            }
        }
    }

    private static byte[] sha256(byte[] bytes) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }
}
