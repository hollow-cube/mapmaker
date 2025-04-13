package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V4306 extends DataVersion {
    public V4306() {
        super(4302);

        removeReference(DataType.ENTITY, "minecraft:potion");

        addReference(DataType.ENTITY, "minecraft:splash_potion", field -> field
                .single("Item", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:lingering_potion", field -> field
                .single("Item", DataType.ITEM_STACK));
    }

}
