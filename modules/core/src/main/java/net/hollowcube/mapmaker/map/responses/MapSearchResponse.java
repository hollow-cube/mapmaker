package net.hollowcube.mapmaker.map.responses;

import net.hollowcube.mapmaker.map.MapData;

import java.util.List;

public record MapSearchResponse(
        int page,
        int pageCount,
        List<MapData> results
) {}
