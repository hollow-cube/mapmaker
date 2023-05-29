package net.hollowcube.mapmaker.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MapQuery(
        @Nullable String query,

        // True --> Map Name, False --> Author UUID
        @Nullable Boolean isQueryMap,

        @Nullable Boolean publishedOnly,

        @Nullable Boolean exactlyMatching
) {

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String query = null;
        private Boolean isQueryMap = null;
        private Boolean publishedOnly = null;
        private Boolean exactlyMatching = null;

        private Builder() {
        }

        public @NotNull Builder query(@NotNull String id) {
            this.query = id;
            return this;
        }

        public @NotNull Builder isQueryMap(boolean isQueryMap) {
            this.isQueryMap = isQueryMap;
            return this;
        }

        public @NotNull Builder publishedOnly(boolean publishedOnly) {
            this.publishedOnly = publishedOnly;
            return this;
        }

        public @NotNull Builder exactlyMatching(boolean exactlyMatching) {
            this.exactlyMatching = exactlyMatching;
            return this;
        }

        public @NotNull MapQuery build() {
            return new MapQuery(query, isQueryMap, publishedOnly, exactlyMatching);
        }
    }

}
