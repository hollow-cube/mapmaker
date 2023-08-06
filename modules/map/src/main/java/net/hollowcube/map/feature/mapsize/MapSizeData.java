package net.hollowcube.map.feature.mapsize;

import net.minestom.server.coordinate.Pos;

public record MapSizeData(Pos mapCenter, double horizontalSize, double verticalSize) {}
