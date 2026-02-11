package net.hollowcube.mapmaker.map.responses;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.map.PlayerTopTimeEntry;

import java.util.List;

@RuntimeGson
public record PlayerTopTimesResponse(int page, int totalItems, List<PlayerTopTimeEntry> items) {
}
