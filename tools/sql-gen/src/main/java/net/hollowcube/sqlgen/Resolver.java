package net.hollowcube.sqlgen;

import com.palantir.javapoet.ClassName;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/// Turns described queries into the [Model] the emitters read: picks row shapes, resolves embeds,
/// applies the nullability directives, and maps every type.
final class Resolver {

    /// Past this many parameters a call site is a row of unlabelled values, so they move into a
    /// record where each one is named at the call.
    private static final int MAX_INLINE_PARAMS = 5;

    /// The select-list `ref.*` markers, in the order they appear. Each one names an embedded table
    /// record the way the query referred to it — the alias if it gave one, else the table name —
    /// and the count has to match the embeds the server describes.
    private static final Pattern STAR = Pattern.compile("(\\w+)\\s*\\.\\s*\\*");

    private final Connection conn;
    private final Catalog catalog;
    private final TypeMap types;
    private final String packageName;
    private final Set<String> nullableEmbeds = new LinkedHashSet<>();

    Resolver(Connection conn, Catalog catalog, TypeMap types, String packageName) {
        this.conn = conn;
        this.catalog = catalog;
        this.types = types;
        this.packageName = packageName;
    }

    Model.Database resolve(String databaseName, List<QueryFile> files) {
        var groups = new ArrayList<Model.Group>();
        for (var file : files) {
            // The interface is named first because the records a query needs are nested in it.
            var interfaceName = ClassName.get(packageName, Names.pascal(file.group()) + "Queries");
            var queries = new ArrayList<Model.Query>();
            for (var query : file.queries()) queries.add(resolve(interfaceName, query));

            groups.add(new Model.Group(
                Names.camel(file.group()),
                interfaceName,
                ClassName.get(packageName, interfaceName.simpleName() + "Impl"),
                List.copyOf(queries)));
        }

        var tables = catalog.tables().values().stream()
            .sorted(Comparator.comparing(Catalog.Table::name)).toList();
        var enums = catalog.enums().values().stream()
            .sorted(Comparator.comparing(Catalog.PgEnum::name)).toList();

        return new Model.Database(packageName, ClassName.get(packageName, databaseName),
            tables, List.copyOf(nullableEmbeds), enums, List.copyOf(groups));
    }

    private Model.Query resolve(ClassName group, QueryFile.Query query) {
        var described = Describe.describe(conn, query);

        var params = new ArrayList<Model.Param>(query.params().size());
        for (int i = 0; i < query.params().size(); i++) {
            var name = query.params().get(i);
            var pgType = described.paramTypes().get(query.binds().indexOf(i));
            // A placeholder used twice is one argument, so both uses have to want the same type.
            for (int bind = 0; bind < query.binds().size(); bind++) {
                if (query.binds().get(bind) != i) continue;
                var other = described.paramTypes().get(bind);
                if (other.equals(pgType)) continue;
                throw new GenException(query.where() + ": placeholder $" + name + " is used as both '"
                    + pgType + "' and '" + other + "'");
            }
            params.add(new Model.Param(name, pgType,
                types.javaType(pgType, false, query.where() + " parameter $" + name)));
        }

        var paramsClass = params.size() > MAX_INLINE_PARAMS
            ? group.nestedClass(Names.pascal(query.name()) + "Params")
            : null;

        return new Model.Query(query.name(), Names.constant(query.name()), query.sql(), query.tag(),
            List.copyOf(params), query.binds(), query.holes(), paramsClass, result(group, query, described));
    }

    private Model.Result result(ClassName group, QueryFile.Query query, Describe.Result described) {
        if (query.tag() == QueryFile.Tag.EXEC) {
            if (!described.columns().isEmpty()) {
                throw new GenException(query.where() + " is tagged :exec but returns columns; use :one or :many");
            }
            return new Model.Result(Model.Shape.NONE, null, List.of());
        }
        if (described.columns().isEmpty()) {
            throw new GenException(query.where() + " returns no columns; tag it :exec");
        }

        var used = new HashSet<String>();
        var components = segment(query, described, used);

        for (var directive : query.nullable()) {
            if (!used.contains(directive)) {
                throw new GenException(query.where() + ": '-- nullable: " + directive
                    + "' does not name a result column or an embedded table");
            }
        }
        for (var directive : query.notNull()) {
            if (!used.contains(directive)) {
                throw new GenException(query.where() + ": '-- not-null: " + directive
                    + "' does not name a result column");
            }
        }

        if (components.size() == 1 && components.getFirst() instanceof Model.Embed embed && !embed.nullable()) {
            return new Model.Result(Model.Shape.TABLE, ClassName.get(packageName, Names.pascal(embed.table().name())),
                components);
        }
        if (components.size() == 1 && components.getFirst() instanceof Model.Value) {
            return new Model.Result(Model.Shape.SCALAR, null, components);
        }
        return new Model.Result(Model.Shape.ROW,
            group.nestedClass(Names.pascal(query.name()) + "Row"), components);
    }

    /// Walks the described columns left to right, collapsing any run that is exactly one table's
    /// columns, in catalog order and under their own names, into a single embedded component. That
    /// run is what `t.*` expands to, and matching it here is what keeps queries sharing one record
    /// per table instead of spawning a near-duplicate each.
    private List<Model.Component> segment(QueryFile.Query query, Describe.Result described, Set<String> used) {
        var aliases = starAliases(query.sql());
        var names = new HashSet<String>();
        var components = new ArrayList<Model.Component>();

        int i = 0;
        int embedIndex = 0;
        while (i < described.columns().size()) {
            var table = embeddedTable(described, i);
            if (table != null) {
                if (embedIndex >= aliases.size()) throw embedMismatch(query, described, aliases);
                var alias = aliases.get(embedIndex++);
                boolean nullable = consume(query.nullable(), used, table.name(), alias);
                if (consume(query.notNull(), used, table.name(), alias)) {
                    throw new GenException(query.where() + ": '-- not-null: " + alias
                        + "' names an embedded table; embeds are not-null unless '-- nullable:' widens them");
                }
                if (nullable) {
                    if (table.primaryKey().isEmpty()) {
                        throw new GenException(query.where() + ": '-- nullable: " + alias + "' needs table '"
                            + table.name() + "' to have a primary key to tell an absent row from a null one");
                    }
                    nullableEmbeds.add(table.name());
                }
                components.add(new Model.Embed(unique(names, Names.camel(alias)), table, i + 1, nullable));
                i += table.columns().size();
                continue;
            }

            var column = described.columns().get(i);
            boolean nullable = nullable(query, column, used);
            var where = query.where() + " column '" + column.label() + "'";
            components.add(new Model.Value(unique(names, Names.camel(column.label())), column.pgType(),
                types.javaType(column.pgType(), nullable, where), nullable, i + 1));
            used.add(column.label().toLowerCase(Locale.ROOT));
            i++;
        }
        if (embedIndex != aliases.size()) throw embedMismatch(query, described, aliases);
        return List.copyOf(components);
    }

    /// Every embedded table must be written as `ref.*` so it has a name; a bare `*`, or a `ref.*`
    /// the server did not describe as one table's full column run, is an error rather than a guess.
    private GenException embedMismatch(QueryFile.Query query, Describe.Result described, List<String> aliases) {
        int embeds = 0;
        for (int i = 0; i < described.columns().size(); ) {
            var table = embeddedTable(described, i);
            if (table == null) { i++; continue; }
            embeds++;
            i += table.columns().size();
        }
        return new GenException(query.where() + ": the server describes " + embeds + " embedded table"
            + (embeds == 1 ? "" : "s") + " but the select list has " + aliases.size() + " 'ref.*' marker"
            + (aliases.size() == 1 ? "" : "s") + " " + aliases
            + "; write each embedded table as alias.* (a bare * is not supported)");
    }

    /// The table whose full column set starts at `i`, or null if the columns there are not one.
    private @Nullable Catalog.Table embeddedTable(Describe.Result described, int i) {
        var first = described.columns().get(i);
        if (first.table() == null) return null;
        var table = catalog.table(first.table());
        if (table == null || i + table.columns().size() > described.columns().size()) return null;

        for (int k = 0; k < table.columns().size(); k++) {
            var column = described.columns().get(i + k);
            var expected = table.columns().get(k);
            if (!table.name().equals(column.table())) return null;
            if (!expected.name().equals(column.baseColumn())) return null;
            // A renamed column is the developer asking for something other than the shared record.
            if (!expected.name().equals(column.label())) return null;
        }
        return table;
    }

    private boolean nullable(QueryFile.Query query, Describe.Column column, Set<String> used) {
        var label = column.label().toLowerCase(Locale.ROOT);
        boolean widen = query.nullable().contains(label);
        boolean narrow = query.notNull().contains(label);
        if (widen && narrow) {
            throw new GenException(query.where() + ": column '" + column.label() + "' is both -- nullable and -- not-null");
        }

        if (narrow) {
            if (column.nullability() != Describe.Nullability.UNKNOWN) {
                throw new GenException(query.where() + ": '-- not-null: " + column.label()
                    + "' is redundant; the server already reports that column as "
                    + (column.nullability() == Describe.Nullability.NOT_NULL ? "not null" : "nullable"));
            }
            used.add(label);
            return false;
        }
        if (widen) {
            if (column.nullability() != Describe.Nullability.NOT_NULL) {
                throw new GenException(query.where() + ": '-- nullable: " + column.label()
                    + "' is redundant; that column is already nullable");
            }
            used.add(label);
            return true;
        }
        // Expression columns describe as unknown, and the server is not going to get more specific.
        return column.nullability() != Describe.Nullability.NOT_NULL;
    }

    private static boolean consume(Set<String> directives, Set<String> used, String table, String alias) {
        boolean matched = false;
        for (var name : List.of(table, alias)) {
            var lower = name.toLowerCase(Locale.ROOT);
            if (!directives.contains(lower)) continue;
            used.add(lower);
            matched = true;
        }
        return matched;
    }

    private static List<String> starAliases(String sql) {
        var matcher = STAR.matcher(sql);
        var out = new ArrayList<String>();
        while (matcher.find()) out.add(matcher.group(1));
        return out;
    }

    private static String unique(Set<String> taken, String name) {
        var candidate = name;
        for (int i = 2; !taken.add(candidate); i++) candidate = name + i;
        return candidate;
    }
}
