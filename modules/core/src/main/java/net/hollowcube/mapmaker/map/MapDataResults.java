package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@RuntimeGson
public record MapDataResults(
        @NotNull List<MapData> results
) {
}
