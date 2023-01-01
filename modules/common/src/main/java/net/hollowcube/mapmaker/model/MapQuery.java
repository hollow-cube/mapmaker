package net.hollowcube.mapmaker.model;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MapQuery(
        @Nullable String author,
        @Nullable Boolean publishedOnly
) {

    public static @NotNull Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String author = null;
        private Boolean publishedOnly = null;

        private Builder() {}

        public @NotNull Builder author(@NotNull String authorId) {
            this.author = authorId;
            return this;
        }

        public @NotNull Builder publishedOnly(boolean publishedOnly) {
            this.publishedOnly = publishedOnly;
            return this;
        }

        public @NotNull MapQuery build() {
            return new MapQuery(author, publishedOnly);
        }
    }

}
