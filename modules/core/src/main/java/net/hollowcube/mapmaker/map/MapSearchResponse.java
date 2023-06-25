package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record MapSearchResponse(
        int page,
        boolean nextPage,
        @NotNull List<PersonalizedMapData> results
) {
}
