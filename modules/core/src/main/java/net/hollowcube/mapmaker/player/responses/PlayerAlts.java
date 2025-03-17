package net.hollowcube.mapmaker.player.responses;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record PlayerAlts(
        @NotNull List<Alt> results
) {

    public record Alt(
            @NotNull String username,
            @NotNull String id
    ) {
    }
}
