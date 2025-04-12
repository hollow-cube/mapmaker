package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1488 extends DataVersion {
    public V1488() {
        super(1488);

        addReference(DataType.BLOCK_ENTITY, "minecraft:command_block", field -> field
                .single("CustomName", DataType.TEXT_COMPONENT)
                .single("LastOutput", DataType.TEXT_COMPONENT));
    }
}
