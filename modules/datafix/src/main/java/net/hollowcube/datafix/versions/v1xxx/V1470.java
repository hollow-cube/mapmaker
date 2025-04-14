package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V1470 extends DataVersion {
    public V1470() {
        super(1470);

        addReference(DataTypes.ENTITY, "minecraft:turtle");
        addReference(DataTypes.ENTITY, "minecraft:cod_mob");
        addReference(DataTypes.ENTITY, "minecraft:tropical_fish");
        addReference(DataTypes.ENTITY, "minecraft:salmon_mob");
        addReference(DataTypes.ENTITY, "minecraft:puffer_fish");
        addReference(DataTypes.ENTITY, "minecraft:phantom");
        addReference(DataTypes.ENTITY, "minecraft:dolphin");
        addReference(DataTypes.ENTITY, "minecraft:drowned");
        addReference(DataTypes.ENTITY, "minecraft:trident", field -> field
                .single("inBlockState", DataTypes.BLOCK_STATE)
                .single("Trident", DataTypes.ITEM_STACK));
    }
}
