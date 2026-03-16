package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record LegacyMapInfo(
    String id,
    String name
) {
}
