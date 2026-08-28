package net.hollowcube.sqlgen.runtime;

import org.jetbrains.annotations.Nullable;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

/// The handful of reads and writes that pgjdbc does not do in one call, factored out so the
/// generator does not inline them into every emitted method.
///
/// Anything pgjdbc already handles directly — `getInt`, `getString`, `getObject(col, UUID.class)` —
/// is emitted as-is and has no helper here.
public final class Jdbc {

    /// Reads a Postgres array as a list. Returns null only for a SQL NULL array; a NULL *element*
    /// stays a null entry in the list.
    @SuppressWarnings("unchecked")
    public static <T> @Nullable List<T> getList(ResultSet rs, int col, Class<T> element) throws SQLException {
        Array array = rs.getArray(col);
        if (array == null) return null;
        try {
            Object[] values = (Object[]) array.getArray();
            List<T> out = new ArrayList<>(values.length);
            for (Object value : values) out.add((T) value);
            return out;
        } finally {
            array.free();
        }
    }

    /// Binds a list as a Postgres array of `pgElementType` (the catalog type name, e.g. `varchar`).
    public static void setList(PreparedStatement ps, int col, String pgElementType, @Nullable List<?> value) throws SQLException {
        if (value == null) {
            ps.setNull(col, Types.ARRAY);
            return;
        }
        ps.setArray(col, ps.getConnection().createArrayOf(pgElementType, value.toArray()));
    }

    /// Reads a `timestamptz`. pgjdbc has no `getObject(col, Instant.class)`, so this goes through
    /// [OffsetDateTime], which it does support and which carries the instant exactly.
    public static @Nullable Instant getInstant(ResultSet rs, int col) throws SQLException {
        OffsetDateTime value = rs.getObject(col, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    public static void setInstant(PreparedStatement ps, int col, @Nullable Instant value) throws SQLException {
        if (value == null) {
            ps.setNull(col, Types.TIMESTAMP_WITH_TIMEZONE);
            return;
        }
        ps.setObject(col, value.atOffset(ZoneOffset.UTC));
    }

    /// Binds a value whose Postgres type the server has to infer from context — enum labels, mostly.
    /// `setString` would send `text` and fail to match the enum type.
    public static void setInferred(PreparedStatement ps, int col, @Nullable String value) throws SQLException {
        ps.setObject(col, value, Types.OTHER);
    }

    private Jdbc() {
    }
}
