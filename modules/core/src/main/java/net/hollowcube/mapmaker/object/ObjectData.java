package net.hollowcube.mapmaker.object;

import net.hollowcube.common.util.RuntimeGson;
import net.minestom.server.coordinate.Point;

@RuntimeGson
public record ObjectData(
    String id,
    ObjectType type,
    Point pos
) {
}
