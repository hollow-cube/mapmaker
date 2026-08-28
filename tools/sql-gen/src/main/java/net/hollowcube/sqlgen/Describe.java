package net.hollowcube.sqlgen;

import org.jetbrains.annotations.Nullable;
import org.postgresql.PGResultSetMetaData;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/// Asks the server what a query takes and returns, by preparing it and reading the metadata back.
///
/// Nothing here interprets SQL. If a query does not describe, it does not compile against this
/// schema, and that is the error the developer wants — reported at generation time with the query
/// named, rather than at 3am against production.
final class Describe {

    /// One result column as the server describes it. [#table] and [#baseColumn] are null for
    /// anything that is not a plain column reference — an expression, an aggregate, a literal.
    record Column(String label, @Nullable String table, @Nullable String baseColumn, String pgType, Nullability nullability) {
    }

    record Result(List<String> paramTypes, List<Column> columns) {
    }

    enum Nullability {
        NOT_NULL, NULLABLE,
        /// Reported for expression columns, where the server does not track it. Treated as nullable
        /// unless `-- not-null:` says otherwise.
        UNKNOWN
    }

    static Result describe(Connection conn, QueryFile.Query query) {
        try (PreparedStatement ps = conn.prepareStatement(query.sql())) {
            var pmd = ps.getParameterMetaData();
            var paramTypes = new ArrayList<String>(pmd.getParameterCount());
            for (int i = 1; i <= pmd.getParameterCount(); i++) paramTypes.add(pmd.getParameterTypeName(i));

            if (paramTypes.size() != query.binds().size()) {
                throw new GenException(query.where() + ": server reports " + paramTypes.size()
                    + " parameters but the query has " + query.binds().size() + " placeholders");
            }

            var columns = new ArrayList<Column>();
            var md = ps.getMetaData();
            if (md != null) {
                var pg = md instanceof PGResultSetMetaData typed ? typed : null;
                for (int i = 1; i <= md.getColumnCount(); i++) {
                    columns.add(new Column(
                        md.getColumnLabel(i),
                        emptyToNull(md.getTableName(i)),
                        pg == null ? null : emptyToNull(pg.getBaseColumnName(i)),
                        md.getColumnTypeName(i),
                        nullability(md.isNullable(i))));
                }
            }
            return new Result(List.copyOf(paramTypes), List.copyOf(columns));
        } catch (SQLException e) {
            throw new GenException(query.where() + " does not describe against the schema: " + e.getMessage(), e);
        }
    }

    private static Nullability nullability(int jdbcNullable) {
        return switch (jdbcNullable) {
            case ResultSetMetaData.columnNoNulls -> Nullability.NOT_NULL;
            case ResultSetMetaData.columnNullable -> Nullability.NULLABLE;
            default -> Nullability.UNKNOWN;
        };
    }

    private static @Nullable String emptyToNull(@Nullable String value) {
        return value == null || value.isEmpty() ? null : value;
    }

    private Describe() {
    }
}
