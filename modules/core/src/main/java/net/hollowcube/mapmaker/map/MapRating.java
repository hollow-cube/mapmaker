package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
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
