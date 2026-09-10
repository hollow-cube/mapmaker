package net.hollowcube.ipc.map;

import org.jetbrains.annotations.Nullable;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/// A page of the published map listing. An empty filter set means any; pages are zero-based.
public record MapSearch(
    int page,
    int pageSize,
    Sort sort,
    boolean ascending,
    Set<MapQuality> qualities,
    Set<MapDifficulty> difficulties,
    Set<MapVariant> variants,
    @Nullable UUID owner,
    @Nullable String query,
    @Nullable UUID contest
) {
    public MapSearch {
        qualities = Set.copyOf(qualities);
        difficulties = Set.copyOf(difficulties);
        variants = Set.copyOf(variants);
    }

    /// `BEST` is quality, then likes, then newest; `PUBLISHED` is newest. `ascending` flips the
    /// quality term of `BEST` and the whole of `PUBLISHED`.
    public enum Sort {
        BEST,
        PUBLISHED,
        UNKNOWN,
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Set<MapQuality> qualities = EnumSet.noneOf(MapQuality.class);
        private final Set<MapDifficulty> difficulties = EnumSet.noneOf(MapDifficulty.class);
        private final Set<MapVariant> variants = EnumSet.noneOf(MapVariant.class);
        private int page = 0;
        private int pageSize = 10;
        private Sort sort = Sort.PUBLISHED;
        private boolean ascending = false;
        private @Nullable UUID owner;
        private @Nullable String query;
        private @Nullable UUID contest;

        private Builder() {}

        public Builder page(int page) {
            this.page = page;
            return this;
        }

        public Builder pageSize(int pageSize) {
            this.pageSize = pageSize;
            return this;
        }

        public Builder sort(Sort sort) {
            this.sort = sort;
            return this;
        }

        public Builder ascending(boolean ascending) {
            this.ascending = ascending;
            return this;
        }

        public Builder qualities(MapQuality... qualities) {
            this.qualities.clear();
            this.qualities.addAll(List.of(qualities));
            return this;
        }

        public Builder difficulties(MapDifficulty... difficulties) {
            this.difficulties.clear();
            this.difficulties.addAll(List.of(difficulties));
            return this;
        }

        public Builder variants(MapVariant... variants) {
            this.variants.clear();
            this.variants.addAll(List.of(variants));
            return this;
        }

        public Builder owner(@Nullable UUID owner) {
            this.owner = owner;
            return this;
        }

        public Builder query(@Nullable String query) {
            this.query = query == null || query.isBlank() ? null : query;
            return this;
        }

        public Builder contest(@Nullable UUID contest) {
            this.contest = contest;
            return this;
        }

        public MapSearch build() {
            return new MapSearch(
                page,
                pageSize,
                sort,
                ascending,
                qualities,
                difficulties,
                variants,
                owner,
                query,
                contest
            );
        }
    }
}
