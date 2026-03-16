package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3083 extends DataVersion {
    public V3083() {
        super(3083);

        addReference(DataTypes.ENTITY, "minecraft:allay", field -> field
            .list("Inventory", DataTypes.ITEM_STACK));
    }
}
