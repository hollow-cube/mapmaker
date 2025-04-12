package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V100 extends DataVersion {
    public V100() {
        super(100);

        addReference(DataType.ENTITY_EQUIPMENT, field -> field
                .list("ArmorItems", DataType.ITEM_STACK)
                .list("HandItems", DataType.ITEM_STACK)
                .single("body_armor_item", DataType.ITEM_STACK)
                .single("saddle", DataType.ITEM_STACK));
    }
}
