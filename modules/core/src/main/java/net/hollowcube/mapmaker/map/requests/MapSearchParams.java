package net.hollowcube.mapmaker.map.requests;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapQuality;
import net.hollowcube.mapmaker.map.MapVariant;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RuntimeGson
public record MapSearchParams(
        @NotNull String authorizer,
        int page,
        int pageSize,
        boolean best,
        boolean ascending,
        @NotNull EnumSet<MapQuality> qualities,
        @NotNull EnumSet<MapData.Difficulty> difficulties,
        @Nullable String owner,
        @Nullable String query,
        @NotNull EnumSet<MapVariant> variants,
        @Nullable String contest
) {

    public String toUrl(@NotNull String url) {
        Map<String, String> params = new HashMap<>();

        params.put("page", String.valueOf(this.page));
        params.put("pageSize", String.valueOf(this.pageSize));
        params.put("sort", this.best ? "best" : "published");
        params.put("sortOrder", this.ascending ? "asc" : "desc");

        params.put("quality", toCSV(MapQuality.class, this.qualities));
        params.put("difficulty", toCSV(MapData.Difficulty.class, this.difficulties));

        params.put("owner", Objects.requireNonNullElse(this.owner, ""));
        params.put("query", Objects.requireNonNullElse(this.query, ""));

        params.put("parkour", String.valueOf(this.variants.contains(MapVariant.PARKOUR)));
        params.put("building", String.valueOf(this.variants.contains(MapVariant.BUILDING)));

        if (this.contest != null) params.put("contest", this.contest);

        return url + "?" + params.entrySet().stream()
                .map(it -> it.getKey() + "=" + URLEncoder.encode(it.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static <T extends Enum<T>> String toCSV(Class<T> type, EnumSet<T> set) {
        if (set.size() == type.getEnumConstants().length) return "";
        return set.stream().map(Enum::name).map(it -> it.toLowerCase(Locale.ROOT)).collect(Collectors.joining(","));
    }

    public static Builder builder(@NotNull String authorizer) {
        return new Builder(authorizer);
    }

    public static class Builder {

        private final @NotNull String authorizer;
        private final EnumSet<MapQuality> qualities = EnumSet.allOf(MapQuality.class);
        private final EnumSet<MapData.Difficulty> difficulties = EnumSet.allOf(MapData.Difficulty.class);
        private final EnumSet<MapVariant> variants = EnumSet.allOf(MapVariant.class);

        private int page = 1;
        private int pageSize = 10;
        private boolean best = false;
        private boolean ascending = false;
        private String owner = null;
        private String query = null;
        private String contest = null;

        private Builder(@NotNull String authorizer) {
            this.authorizer = authorizer;
        }

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder best(boolean best) {
            this.best = best;
            return this;
        }

        public Builder ascending(boolean ascending) {
            this.ascending = ascending;
            return this;
        }

        public Builder qualities(@NotNull MapQuality... qualities) {
            this.qualities.clear();
            this.qualities.addAll(List.of(qualities));
            return this;
        }

        public Builder difficulties(@NotNull MapData.Difficulty... difficulties) {
            this.difficulties.clear();
            this.difficulties.addAll(List.of(difficulties));
            return this;
        }

        public Builder owner(@Nullable String owner) {
            this.owner = owner;
            return this;
        }

        public Builder query(@Nullable String query) {
            this.query = query;
            return this;
        }

        public Builder variants(@NotNull MapVariant... variants) {
            this.variants.clear();
            this.variants.addAll(variants.length == 0 ? EnumSet.allOf(MapVariant.class) : List.of(variants));
            return this;
        }

        public Builder contest(@Nullable String contest) {
            this.contest = contest;
            return this;
        }

        public MapSearchParams build() {
            return new MapSearchParams(authorizer, page, pageSize, best, ascending, qualities,
                                       difficulties, owner, query, variants, contest);
        }
    }
}
