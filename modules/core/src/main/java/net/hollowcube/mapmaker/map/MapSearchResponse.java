package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RuntimeGson
public record MapSearchResponse<M extends MapData>(
        int page,
        boolean nextPage,
        @NotNull List<M> results
) {
}
