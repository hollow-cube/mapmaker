package net.hollowcube.datafix.versions;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1470 extends DataVersion {
    public V1470() {
        super(1470);

        addReference(DataType.ENTITY, "minecraft:turtle");
        addReference(DataType.ENTITY, "minecraft:cod_mob");
        addReference(DataType.ENTITY, "minecraft:tropical_fish");
        addReference(DataType.ENTITY, "minecraft:salmon_mob");
        addReference(DataType.ENTITY, "minecraft:puffer_fish");
        addReference(DataType.ENTITY, "minecraft:phantom");
        addReference(DataType.ENTITY, "minecraft:dolphin");
        addReference(DataType.ENTITY, "minecraft:drowned");
        addReference(DataType.ENTITY, "minecraft:trident", field -> field
                .single("inBlockState", DataType.BLOCK_STATE)
                .single("Trident", DataType.ITEM_STACK));
    }
}
