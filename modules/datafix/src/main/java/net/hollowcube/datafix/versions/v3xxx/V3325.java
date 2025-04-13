package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3325 extends DataVersion {
    public V3325() {
        super(3325);

        addReference(DataType.ENTITY, "minecraft:item_display", field -> field
                .single("item", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:block_display", field -> field
                .single("block_state", DataType.BLOCK_STATE));
        addReference(DataType.ENTITY, "minecraft:text_display", field -> field
                .single("text", DataType.TEXT_COMPONENT));
    }
}
