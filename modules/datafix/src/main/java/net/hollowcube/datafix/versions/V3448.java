package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3448 extends DataVersion {
    public V3448() {
        super(3448);

        addReference(DataType.BLOCK_ENTITY, "minecraft:decorated_pot", field -> field
                .list("sherts", DataType.ITEM_NAME)
                .single("item", DataType.ITEM_STACK));
    }
}
