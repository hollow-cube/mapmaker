package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;

@RuntimeGson
public record PlayerTopTimeEntry(String mapId, String mapName, int publishedId, int completionTime, int rank) {
}
