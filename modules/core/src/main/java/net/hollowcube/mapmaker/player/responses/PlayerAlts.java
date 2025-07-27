package net.hollowcube.mapmaker.player.responses;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RuntimeGson
public record PlayerAlts(
        @NotNull List<Alt> results
) {

    @RuntimeGson
    public record Alt(
            @NotNull String username,
            @NotNull String id
    ) {
    }
}
