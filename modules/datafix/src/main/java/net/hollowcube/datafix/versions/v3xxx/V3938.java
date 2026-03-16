package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V3938 extends DataVersion {
    public V3938() {
        super(3938);

        addReference(DataTypes.ENTITY, "minecraft:spectral_arrow", V3938::abstractArrow);
        addReference(DataTypes.ENTITY, "minecraft:arrow", V3938::abstractArrow);

        // TODO there is one of those writeReadFix cases here, need to again figure out what that does...
    }

    static DataType.Builder abstractArrow(DataType.Builder field) {
        return field
            .single("inBlockState", DataTypes.BLOCK_STATE)
            .single("item", DataTypes.ITEM_STACK)
            .single("weapon", DataTypes.ITEM_STACK);
    }
}
