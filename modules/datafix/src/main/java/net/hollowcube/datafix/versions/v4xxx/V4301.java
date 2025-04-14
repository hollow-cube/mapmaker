package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4301 extends DataVersion {
    public V4301() {
        super(4301);

        addReference(DataTypes.ENTITY_EQUIPMENT, field -> field
                .single("equipment.mainhand", DataTypes.ITEM_STACK)
                .single("equipment.offhand", DataTypes.ITEM_STACK)
                .single("equipment.feet", DataTypes.ITEM_STACK)
                .single("equipment.legs", DataTypes.ITEM_STACK)
                .single("equipment.chest", DataTypes.ITEM_STACK)
                .single("equipment.head", DataTypes.ITEM_STACK)
                .single("equipment.body", DataTypes.ITEM_STACK)
                .single("equipment.saddle", DataTypes.ITEM_STACK));
    }

}
