package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V3090 extends DataVersion {
    public V3090() {
        super(3090);

        addFix(DataType.ENTITY, "minecraft:painting", V3090::fixPaintingFieldNames);
    }

    private static Value fixPaintingFieldNames(Value entity) {
        entity.put("variant", entity.remove("Motive"));
        entity.put("facing", entity.remove("Facing"));
        return null;
    }
}
