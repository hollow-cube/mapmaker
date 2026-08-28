package net.hollowcube.sqlgen.runtime;

import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/// A piece of SQL the caller writes, spliced into a generated query at a `/* where */` or
/// `/* order by */` hole.
///
/// This is the deliberate seam between generated code and query building, and the only part of a
/// query the generator does not describe against the schema. The convention that keeps it safe is
/// that [#sql] is developer-written text — a constant, or something assembled from constants — and
/// everything that came from a user goes in [#params].
public record SqlFragment(String sql, List<Object> params) {

    public SqlFragment {
        params = List.copyOf(params);
    }

    public static SqlFragment of(String sql, Object... params) {
        return new SqlFragment(sql, List.of(params));
    }

    /// The fragment as a clause, or a single space when there is no fragment. Generated code splices
    /// this between the two halves of the statement, so it always leaves the tokens either side
    /// separated.
    public static String clause(String keyword, @Nullable SqlFragment fragment) {
        return fragment == null ? " " : " " + keyword + " " + fragment.sql() + " ";
    }

    /// Binds the fragment's parameters starting at `index`, and returns the next free index.
    public static int bind(PreparedStatement ps, int index, @Nullable SqlFragment fragment) throws SQLException {
        if (fragment == null) return index;
        for (Object param : fragment.params()) ps.setObject(index++, param);
        return index;
    }
}
