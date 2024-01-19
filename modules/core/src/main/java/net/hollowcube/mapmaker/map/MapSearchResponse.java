package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record MapSearchResponse<M extends MapData>(
        int page,
        boolean nextPage,
        @NotNull List<M> results
) {
}
