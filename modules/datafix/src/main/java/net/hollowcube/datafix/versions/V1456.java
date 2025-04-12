package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V1456 extends DataVersion {
    public V1456() {
        super(1456);

        addFix(DataType.ENTITY, "minecraft:item_frame", V1456::fixItemFrameDirection);
    }

    private static Value fixItemFrameDirection(Value value) {
        byte facing = value.get("Facing").as(Number.class, 0).byteValue();
        value.put("Facing", (byte) switch (facing) {
            case 0 -> 3;
            case 1 -> 4;
            case 3 -> 5;
            default -> 2;
        });
        return null;
    }
}
