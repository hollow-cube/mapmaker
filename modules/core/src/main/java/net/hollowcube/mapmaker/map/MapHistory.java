package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record MapHistory(
        int page,
        boolean nextPage,
        @NotNull List<Entry> results
) {

    public record Entry(
            @NotNull String mapId
    ) {

    }
}
