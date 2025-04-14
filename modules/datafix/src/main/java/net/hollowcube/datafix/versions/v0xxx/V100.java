package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V100 extends DataVersion {
    public V100() {
        super(100);

        addReference(DataTypes.ENTITY_EQUIPMENT, field -> field
                .list("ArmorItems", DataTypes.ITEM_STACK)
                .list("HandItems", DataTypes.ITEM_STACK)
                .single("body_armor_item", DataTypes.ITEM_STACK)
                .single("saddle", DataTypes.ITEM_STACK));

        // TODO entity equipment fixes
    }
}
