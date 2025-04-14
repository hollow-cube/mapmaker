package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V4306 extends DataVersion {
    public V4306() {
        super(4302);

        removeReference(DataTypes.ENTITY, "minecraft:potion");

        addReference(DataTypes.ENTITY, "minecraft:splash_potion", field -> field
                .single("Item", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:lingering_potion", field -> field
                .single("Item", DataTypes.ITEM_STACK));
    }

}
