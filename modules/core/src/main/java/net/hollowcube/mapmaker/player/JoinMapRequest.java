package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record JoinMapRequest(
        @NotNull String player,
        @NotNull String map,
        @NotNull String state,
        @NotNull String source,
        @Nullable Isolate isolate
) {

    public record Isolate(
            @Nullable String override
    ) {
    }

    public JoinMapRequest(
            @NotNull String player,
            @NotNull String map,
            @NotNull String state,
            @NotNull String source
    ) {
        this(player, map, state, source, (Isolate) null);
    }

    public JoinMapRequest(
            @NotNull String player,
            @NotNull String map,
            @NotNull String state,
            @NotNull String source,
            @Nullable String isolateOverride
    ) {
        this(player, map, state, source, new Isolate(isolateOverride));
    }
}
