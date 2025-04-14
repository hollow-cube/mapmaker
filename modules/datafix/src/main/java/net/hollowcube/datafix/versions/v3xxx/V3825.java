package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3825 extends DataVersion {
    public V3825() {
        super(3825);

        addReference(DataTypes.ENTITY, "minecraft:ominous_item_spawner", field -> field
                .single("item", DataTypes.ITEM_STACK));
    }

}
