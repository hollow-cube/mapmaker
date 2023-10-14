package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MapSearchRequest(
        @NotNull String authorizer,
        int page,
        int pageSize,
        @Nullable String owner
) {

    public static @NotNull Builder builder(@NotNull String authorizer) {
        return new Builder(authorizer);
    }

    public static class Builder {
        private final String authorizer;

        private int page = -1;
        private int pageSize = -1;

        private String owner = null;


        Builder(@NotNull String authorizer) {
            this.authorizer = authorizer;
        }

        public @NotNull Builder page(int page, int pageSize) {
            this.page = page;
            this.pageSize = pageSize;
            return this;
        }

        public @NotNull Builder owner(@NotNull String owner) {
            this.owner = owner;
            return this;
        }

        public @NotNull MapSearchRequest build() {
            return new MapSearchRequest(
                    authorizer, page, pageSize, owner
            );
        }
    }

}
