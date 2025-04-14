package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V110 extends DataVersion {

    public V110() {
        super(0);

        addFix(DataTypes.ENTITY, "EntityHorse", V110::fixHorseSaddle);
    }

    private static Value fixHorseSaddle(Value horse) {
        if (!horse.get("Saddle").as(Boolean.class, false))
            return null;

        var saddleItem = Value.emptyMap();
        saddleItem.put("id", "minecraft:saddle");
        saddleItem.put("Count", (byte) 1);
        saddleItem.put("Damage", (short) 0);

        horse.put("SaddleItem", saddleItem);
        horse.put("Saddle", null);
        return null;
    }

}
