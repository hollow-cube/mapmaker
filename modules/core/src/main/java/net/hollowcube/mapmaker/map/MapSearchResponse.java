package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;

import java.util.List;

@RuntimeGson
public record MapSearchResponse<M extends MapData>(
    int page,
    boolean nextPage,
    List<M> results
) {
}
