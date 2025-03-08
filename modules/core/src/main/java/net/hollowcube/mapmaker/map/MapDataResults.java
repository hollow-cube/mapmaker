package net.hollowcube.mapmaker.map;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public record MapDataResults(
        @NotNull List<MapData> results
) {}
