package net.hollowcube.mapmaker.map.util.spatial;

import org.jetbrains.annotations.NotNullByDefault;

// Intentionally NOT a record because we use identity comparisons in tests.
@NotNullByDefault
@SuppressWarnings("ClassCanBeRecord")
public final class SimpleSpatialObject implements SpatialObject {
    private final BoundingBox boundingBox;

    public SimpleSpatialObject(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public SimpleSpatialObject(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this(new BoundingBox(minX, minY, minZ, maxX, maxY, maxZ));
    }

    @Override
    public BoundingBox boundingBox() {
        return boundingBox;
    }
}
