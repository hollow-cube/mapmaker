package net.hollowcube.mapmaker.map.responses;

import net.hollowcube.mapmaker.map.MapCollection;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public record MapCollectionsResponse(
        @NotNull List<MapCollection> results
) {
}
