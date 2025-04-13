package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3083 extends DataVersion {
    public V3083() {
        super(3083);

        addReference(DataType.ENTITY, "minecraft:allay", field -> field
                .list("Inventory", DataType.ITEM_STACK));
    }
}
