package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.Nullable;

@RuntimeGson
public record JoinMapRequest(
    String player,
    String map,
    String state,
    String source,
    @Nullable Isolate isolate
) {

    public record Isolate(
        @Nullable String override
    ) {
    }

    public JoinMapRequest(
        String player,
        String map,
        String state,
        String source
    ) {
        this(player, map, state, source, (Isolate) null);
    }

    public JoinMapRequest(
        String player,
        String map,
        String state,
        String source,
        @Nullable String isolateOverride
    ) {
        this(player, map, state, source, new Isolate(isolateOverride));
    }
}
