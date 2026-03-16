package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;

import java.util.List;

@RuntimeGson
public record MapDataResults(
    List<MapData> results
) {
}
