package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3325 extends DataVersion {
    public V3325() {
        super(3325);

        addReference(DataTypes.ENTITY, "minecraft:item_display", field -> field
                .single("item", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:block_display", field -> field
                .single("block_state", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:text_display", field -> field
                .single("text", DataTypes.TEXT_COMPONENT));
    }
}
