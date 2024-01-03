package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record MapRating(@NotNull State state, @Nullable String comment) {

    public MapRating() {
        this(State.UNRATED, null);
    }

    public enum State {
        UNRATED,
        LIKED,
        DISLIKED
    }
}
