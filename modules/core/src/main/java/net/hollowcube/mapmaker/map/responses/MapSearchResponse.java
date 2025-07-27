package net.hollowcube.mapmaker.map.responses;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.map.MapData;

import java.util.List;

@RuntimeGson
public record MapSearchResponse(
        int page,
        int pageCount,
        List<MapData> results
) {
}
