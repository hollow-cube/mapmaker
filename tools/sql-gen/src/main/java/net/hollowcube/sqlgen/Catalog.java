package net.hollowcube.sqlgen;

import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/// What the migrations built, read back out of `pg_catalog`: the tables that become records and the
/// enum types that become Java enums.
record Catalog(Map<String, Table> tables, Map<String, PgEnum> enums) {

    record Table(String name, List<Column> columns, List<String> primaryKey) {
    }

    record Column(String name, String pgType, boolean notNull) {
    }

    record PgEnum(String name, List<String> labels) {
    }

    @Nullable Table table(String name) {
        return tables.get(name);
    }

    @Nullable PgEnum pgEnum(String pgType) {
        return enums.get(pgType);
    }

    private static final String TABLES = """
        select c.relname, a.attname, t.typname, a.attnotnull
        from pg_class c
            join pg_namespace n on n.oid = c.relnamespace
            join pg_attribute a on a.attrelid = c.oid
            join pg_type t on t.oid = a.atttypid
        where n.nspname = 'public' and c.relkind = 'r' and a.attnum > 0 and not a.attisdropped
        order by c.relname, a.attnum""";

    private static final String PRIMARY_KEYS = """
        select c.relname, a.attname
        from pg_index i
            join pg_class c on c.oid = i.indrelid
            join pg_namespace n on n.oid = c.relnamespace
            join pg_attribute a on a.attrelid = c.oid and a.attnum = any(i.indkey)
        where i.indisprimary and n.nspname = 'public'
        order by c.relname, a.attnum""";

    private static final String ENUMS = """
        select t.typname, e.enumlabel
        from pg_type t
            join pg_enum e on e.enumtypid = t.oid
            join pg_namespace n on n.oid = t.typnamespace
        where n.nspname = 'public'
        order by t.typname, e.enumsortorder""";

    static Catalog read(Connection conn) throws SQLException {
        var columns = new LinkedHashMap<String, List<Column>>();
        var primaryKeys = new LinkedHashMap<String, List<String>>();
        var labels = new LinkedHashMap<String, List<String>>();

        try (Statement st = conn.createStatement()) {
            try (var rs = st.executeQuery(TABLES)) {
                while (rs.next()) {
                    columns.computeIfAbsent(rs.getString(1), key -> new ArrayList<>())
                        .add(new Column(rs.getString(2), rs.getString(3), rs.getBoolean(4)));
                }
            }
            try (var rs = st.executeQuery(PRIMARY_KEYS)) {
                while (rs.next()) {
                    primaryKeys.computeIfAbsent(rs.getString(1), key -> new ArrayList<>()).add(rs.getString(2));
                }
            }
            try (var rs = st.executeQuery(ENUMS)) {
                while (rs.next()) {
                    labels.computeIfAbsent(rs.getString(1), key -> new ArrayList<>()).add(rs.getString(2));
                }
            }
        }

        var tables = new LinkedHashMap<String, Table>();
        columns.forEach((name, cols) ->
            tables.put(name, new Table(name, List.copyOf(cols), List.copyOf(primaryKeys.getOrDefault(name, List.of())))));

        var enums = new LinkedHashMap<String, PgEnum>();
        labels.forEach((name, values) -> enums.put(name, new PgEnum(name, List.copyOf(values))));

        return new Catalog(Map.copyOf(tables), Map.copyOf(enums));
    }
}
