package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3082 extends DataVersion {
    public V3082() {
        super(3082);

        addReference(DataTypes.ENTITY, "minecraft:chest_boat", field -> field
            .list("Items", DataTypes.ITEM_STACK));
    }
}
