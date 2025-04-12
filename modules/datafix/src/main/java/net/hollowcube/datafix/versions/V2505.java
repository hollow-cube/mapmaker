package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V2505 extends DataVersion {
    public V2505() {
        super(2505);

        addReference(DataType.ENTITY, "minecraft:piglin", field -> field
                .list("Inventory", DataType.ITEM_STACK));
    }
}
