package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3202 extends DataVersion {
    public V3202() {
        super(3202);

        addReference(DataType.BLOCK_ENTITY, "minecraft:hanging_sign", V99::signBlock);
    }
}
