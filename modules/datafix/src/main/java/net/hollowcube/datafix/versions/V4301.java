package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V4301 extends DataVersion {
    public V4301() {
        super(4301);

        addReference(DataType.ENTITY_EQUIPMENT, field -> field
                .single("equipment.mainhand", DataType.ITEM_STACK)
                .single("equipment.offhand", DataType.ITEM_STACK)
                .single("equipment.feet", DataType.ITEM_STACK)
                .single("equipment.legs", DataType.ITEM_STACK)
                .single("equipment.chest", DataType.ITEM_STACK)
                .single("equipment.head", DataType.ITEM_STACK)
                .single("equipment.body", DataType.ITEM_STACK)
                .single("equipment.saddle", DataType.ITEM_STACK));
    }

}
