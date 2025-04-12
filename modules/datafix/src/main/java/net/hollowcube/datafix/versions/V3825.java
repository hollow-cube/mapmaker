package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V3825 extends DataVersion {
    public V3825() {
        super(3825);

        addReference(DataType.ENTITY, "minecraft:ominous_item_spawner", field -> field
                .single("item", DataType.ITEM_STACK));
    }

}
