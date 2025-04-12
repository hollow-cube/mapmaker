package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V111 extends DataVersion {
    private static final int[][] DIRECTIONS = new int[][]{{0, 0, 1}, {-1, 0, 0}, {0, 0, -1}, {1, 0, 0}};

    public V111() {
        super(111);

        addFix(DataType.ENTITY, "Painting", value -> fixPaintingOrItemFrameDirection(value, false));
        addFix(DataType.ENTITY, "ItemFrame", value -> fixPaintingOrItemFrameDirection(value, true));
    }

    private static Value fixPaintingOrItemFrameDirection(Value value, boolean isItemFrame) {
        if (value.getValue("Facing") != null) return null;

        int facing;
        if (value.getValue("Direction") instanceof Number n) {
            facing = n.byteValue() % DIRECTIONS.length;
            int[] direction = DIRECTIONS[facing];
            value.put("TileX", value.get("TileX").as(Number.class, 0).intValue() + direction[0]);
            value.put("TileY", value.get("TileY").as(Number.class, 0).intValue() + direction[1]);
            value.put("TileZ", value.get("TileZ").as(Number.class, 0).intValue() + direction[2]);
            value.put("Direction", null);

            if (isItemFrame && value.getValue("ItemRotation") instanceof Number itemRotation) {
                value.put("ItemRotation", (byte) (itemRotation.byteValue() * 2));
            }
        } else {
            facing = value.get("Dir").as(Number.class, 0).byteValue() % DIRECTIONS.length;
            value.put("Dir", null);
        }

        value.put("Facing", (byte) facing);
        return null;
    }
}
