package net.hollowcube.sqlgen;

import java.util.List;
import java.util.Set;

/// One parsed `.sql` file of queries. The file name is the group: `heads.sql` becomes the
/// `HeadsQueries` interface and the `heads` field on the database.
record QueryFile(String group, List<Query> queries) {

    /// One `-- name:` block, with `$param` placeholders already rewritten to JDBC `?`.
    record Query(
        String name,
        Tag tag,
        /// The statement as it will be prepared: `?` in place of every `$param`.
        String sql,
        /// Distinct placeholder names in order of first appearance; these are the method arguments.
        List<String> params,
        /// One entry per `?` in [#sql], holding the index into [#params] it binds. A placeholder
        /// used twice appears twice here.
        List<Integer> binds,
        /// Columns `-- nullable:` widens.
        Set<String> nullable,
        /// Columns `-- not-null:` narrows.
        Set<String> notNull,
        /// The `/* where */` and `/* order by */` markers, in the order they appeared.
        List<Hole> holes,
        /// Line the `-- name:` header sits on, for error messages.
        int line,
        /// A `:one` whose select list is nothing but aggregates: there is always exactly one row,
        /// so there is no "no row" for the result to be null over.
        boolean alwaysOneRow
    ) {

        String where() {
            return "query '" + name + "' (line " + line + ")";
        }
    }

    /// A place the caller can splice their own SQL into. The marker itself is stripped from
    /// [Query#sql], so the statement still describes with nothing filled in.
    record Hole(
        Kind kind,
        /// Offset into [Query#sql] the marker sat at.
        int sqlOffset,
        /// How many `?` come before the hole, which is where the fragment's own parameters go.
        int bindOffset
    ) {

        enum Kind {
            WHERE("where", "where"),
            ORDER_BY("order by", "orderBy");

            final String keyword;
            final String argument;

            Kind(String keyword, String argument) {
                this.keyword = keyword;
                this.argument = argument;
            }

            static @org.jetbrains.annotations.Nullable Kind of(String marker) {
                return switch (marker) {
                    case "where" -> WHERE;
                    case "order by" -> ORDER_BY;
                    default -> null;
                };
            }
        }
    }

    enum Tag {
        ONE, MANY, EXEC;

        static Tag parse(String text) {
            return switch (text) {
                case "one" -> ONE;
                case "many" -> MANY;
                case "exec" -> EXEC;
                default -> throw new GenException("unknown query tag ':" + text + "', expected :one :many or :exec");
            };
        }
    }
}
