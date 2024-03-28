package net.hollowcube.mapmaker.session;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record SessionStateUpdateRequest(
        @Nullable Boolean hidden,

        @NotNull Metadata metadata
) {

    public static @NotNull SessionStateUpdateRequest hidden(boolean newValue, boolean silent) {
        return new SessionStateUpdateRequest(newValue, new Metadata(silent));
    }

    public record Metadata(@Nullable Boolean hideSilent) {

        public Metadata() {
            this(null);
        }
    }
}
