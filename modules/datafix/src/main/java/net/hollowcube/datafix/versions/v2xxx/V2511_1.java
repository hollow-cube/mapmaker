package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V2511_1 extends DataVersion {
    public V2511_1() {
        super(2511); // todo what is id

        addReference(DataTypes.ENTITY, "minecraft:potion", field -> field
                .single("Item", DataTypes.ITEM_STACK));
    }
}
