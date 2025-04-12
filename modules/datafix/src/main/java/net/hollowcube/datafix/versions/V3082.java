package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3082 extends DataVersion {
    public V3082() {
        super(3082);

        addReference(DataType.ENTITY, "minecraft:chest_boat", field -> field
                .list("Items", DataType.ITEM_STACK));
    }
}
