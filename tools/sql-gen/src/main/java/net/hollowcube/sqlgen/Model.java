package net.hollowcube.sqlgen;

import com.palantir.javapoet.ClassName;
import com.palantir.javapoet.TypeName;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/// Everything the emitters need, with no Postgres left in it except the type names the bindings are
/// keyed on. Built by [Resolver] from the parsed queries plus what the server said about them.
final class Model {

    record Database(
        String packageName,
        ClassName className,
        List<Catalog.Table> tables,
        /// Tables that need a null-tolerant mapper because some query embeds them across an outer
        /// join.
        List<String> nullableEmbeds,
        List<Catalog.PgEnum> enums,
        List<Group> groups
    ) {
    }

    /// One query file: an interface, a package-private implementation of it, and a field on the
    /// database named after the file.
    record Group(String fieldName, ClassName interfaceName, ClassName implName, List<Query> queries) {
    }

    record Query(
        String name,
        String constantName,
        String sql,
        QueryFile.Tag tag,
        List<Param> params,
        /// One entry per `?` in the SQL, holding the index into [#params] it binds. A placeholder
        /// used twice is one argument bound twice.
        List<Integer> binds,
        /// The `/* where */` and `/* order by */` markers this query left open, in the order they
        /// appear. Each becomes a trailing [net.hollowcube.sqlgen.runtime.SqlFragment] argument.
        List<QueryFile.Hole> holes,
        /// Set once a query takes more arguments than read well in a call: the parameters move into
        /// a record nested in the group interface, and the method takes one of those instead.
        @Nullable ClassName paramsClass,
        Result result,
        /// A `:one` that always finds its row, so it is returned as is rather than as null-or-row.
        boolean exactlyOne
    ) {
    }

    record Param(String name, String pgType, TypeName type) {
    }

    /// The shape of one row, and how to build it from a [java.sql.ResultSet]. A [Shape#ROW] row
    /// class is nested in the group interface; a [Shape#TABLE] one is the shared table record.
    record Result(Shape shape, @Nullable ClassName rowClass, List<Component> components) {
    }

    enum Shape {
        /// `:exec` — an update count, no columns.
        NONE,
        /// A single column, returned unwrapped rather than in a one-component record.
        SCALAR,
        /// Exactly one table's full column set, returned as that table's record.
        TABLE,
        /// A generated per-query record.
        ROW
    }

    sealed interface Component {

        String name();

        /// 1-based index of the first result column this component reads.
        int column();
    }

    /// A `t.*` segment, mapped through the table's shared record.
    record Embed(String name, Catalog.Table table, int column, boolean nullable) implements Component {
    }

    record Value(String name, String pgType, TypeName type, boolean nullable, int column) implements Component {
    }

    private Model() {
    }
}
