package net.hollowcube.apiserver.map;

import net.hollowcube.ipc.map.MapDifficulty;
import net.hollowcube.ipc.map.MapQuality;
import net.hollowcube.ipc.map.MapSearch;
import net.hollowcube.ipc.map.MapVariant;
import net.hollowcube.sqlgen.runtime.SqlFragment;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

import static java.util.Collections.nCopies;

/// The two fragments `searchMaps` leaves to its caller: the filters, so that the planner only sees
/// the ones asked for, and the sort. Every value a caller sent is a bind; the text is constants.
final class MapSearchSql {

    /// Go's `maps_published`: what the listing shows at all.
    private static final String LISTED = "maps.deleted_at is null"
        + " and maps.published_at is not null"
        + " and maps.published_id is not null"
        + " and maps.listed";

    /// The view's difficulty bucket, which reads the `map_stats` join the query already makes.
    private static final String DIFFICULTY = "(case"
        + " when coalesce(stats.play_count, 0) < 10 then -1"
        + " when stats.clear_rate < 0.05 then 4"
        + " when stats.clear_rate < 0.25 then 3"
        + " when stats.clear_rate < 0.5 then 2"
        + " when stats.clear_rate < 0.75 then 1"
        + " else 0 end)";

    private MapSearchSql() {}

    static SqlFragment where(MapSearch search) {
        var sql = new StringBuilder(LISTED);
        var params = new ArrayList<>();
        if (search.owner() != null) {
            sql.append(" and maps.owner = ?");
            params.add(search.owner());
        }
        if (search.contest() != null) {
            sql.append(" and maps.contest = ?");
            params.add(search.contest());
        }
        if (search.query() != null) {
            // `%` and `_` in the query are wildcards, as they were for Go.
            sql.append(" and maps.opt_name ilike ?");
            params.add("%" + search.query() + "%");
        }
        in(
            sql,
            params,
            "maps.opt_variant",
            search.variants(),
            v -> v.name().toLowerCase(Locale.ROOT)
        );
        in(sql, params, "maps.quality_override", search.qualities(), q -> (long) q.ordinal());
        in(sql, params, DIFFICULTY, search.difficulties(), MapCompat::difficultyId);
        return new SqlFragment(sql.toString(), params);
    }

    /// Go's order is not total, so `id` closes it and pages stay stable.
    static SqlFragment orderBy(MapSearch search) {
        var direction = search.ascending() ? " asc" : " desc";
        return switch (search.sort()) {
            case BEST -> SqlFragment.of(
                "maps.quality_override"
                    + direction
                    + ", maps.total_likes desc, maps.published_at desc, maps.id"
            );
            case PUBLISHED -> SqlFragment.of("maps.published_at" + direction + ", maps.id");
            case UNKNOWN -> throw new IllegalArgumentException("unknown sort");
        };
    }

    /// `column in (?, ?)` for a non-empty set; an empty set is no filter.
    private static <T> void in(
        StringBuilder sql,
        List<Object> params,
        String column,
        Collection<T> values,
        Function<T, Object> bind
    ) {
        if (values.isEmpty()) return;
        sql.append(" and ")
            .append(column)
            .append(" in (")
            .append(String.join(", ", nCopies(values.size(), "?")))
            .append(")");
        for (var value : values) params.add(bind.apply(value));
    }
}
