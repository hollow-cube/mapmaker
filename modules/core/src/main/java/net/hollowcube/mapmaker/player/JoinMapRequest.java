package net.hollowcube.mapmaker.player;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

@RuntimeGson
public record JoinMapRequest(
        @NotNull String player,
        @NotNull String map,
        @NotNull String state,
        @NotNull String source
) {
}
