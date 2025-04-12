package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1800 extends DataVersion {
    public V1800() {
        super(1800);

        addReference(DataType.ENTITY, "minecraft:panda");
        addReference(DataType.ENTITY, "minecraft:pillager", field -> field
                .list("Inventory", DataType.ITEM_STACK));
    }
}
