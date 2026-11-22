package net.hollowcube.sqlgen;

import net.hollowcube.sqlgen.runtime.Migrator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Pattern;

/// Drops applied on top of the migrations for generation and tests but never to a real database, so
/// code stops reading a column one deploy before a migration removes it.
///
/// Only drops are allowed: anything else would make the generated code expect a schema the running
/// database does not have. A dropped column must also be nullable or have a default, since inserts
/// generated without it still run against the real table.
final class Overlay {

    private static final Pattern COMMENT = Pattern.compile("--[^\n]*");
    private static final Pattern DROP_COLUMN = Pattern.compile(
        "(?is)alter\\s+table\\s+(?:if\\s+exists\\s+)?(?:only\\s+)?(\\S+)\\s+drop\\s+(?:column\\s+)?(?:if\\s+exists\\s+)?(\\S+)(?:\\s+(?:cascade|restrict))?");
    private static final Pattern DROP_OBJECT = Pattern.compile("(?is)drop\\s+(?:table|index|type|view)\\s+.+");

    private Overlay() {
    }

    static void apply(Connection conn, Path file) throws SQLException {
        if (!Files.isRegularFile(file)) throw new GenException("no overlay file at " + file);
        String sql;
        try {
            sql = Files.readString(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        for (var piece : Migrator.pgliteStatements(sql)) {
            for (var statement : COMMENT.matcher(piece).replaceAll("").split(";")) {
                statement = statement.strip();
                if (statement.isEmpty()) continue;
                check(conn, file, statement);
                try (var st = conn.createStatement()) {
                    st.execute(statement);
                } catch (SQLException e) {
                    throw new GenException(file.getFileName() + ": '" + statement + "' failed: " + e.getMessage(), e);
                }
            }
        }
    }

    private static void check(Connection conn, Path file, String statement) throws SQLException {
        var column = DROP_COLUMN.matcher(statement);
        if (column.matches()) {
            var table = identifier(column.group(1));
            var name = identifier(column.group(2));
            try (var ps = conn.prepareStatement("""
                select not a.attnotnull or a.atthasdef or a.attidentity <> '' or a.attgenerated <> ''
                from pg_attribute a
                         join pg_class c on c.oid = a.attrelid
                         join pg_namespace n on n.oid = c.relnamespace
                where n.nspname = 'public' and c.relname = ? and a.attname = ? and a.attnum > 0 and not a.attisdropped""")) {
                ps.setString(1, table);
                ps.setString(2, name);
                try (var rs = ps.executeQuery()) {
                    if (!rs.next())
                        throw new GenException(file.getFileName() + " drops " + table + "." + name + ", which does not exist");
                    if (!rs.getBoolean(1))
                        throw new GenException(file.getFileName() + " drops " + table + "." + name
                            + ", which is not null without a default; inserts that leave it out would fail on the real database");
                }
            }
            return;
        }
        if (DROP_OBJECT.matcher(statement).matches()) return;
        throw new GenException(file.getFileName() + " may only drop columns, tables, indexes, types and views; found '"
            + statement + "'");
    }

    private static String identifier(String raw) {
        var name = raw.startsWith("public.") ? raw.substring("public.".length()) : raw;
        return name.startsWith("\"") && name.endsWith("\"")
            ? name.substring(1, name.length() - 1)
            : name.toLowerCase(Locale.ROOT);
    }
}
