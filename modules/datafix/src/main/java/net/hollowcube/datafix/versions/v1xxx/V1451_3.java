package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;

public class V1451_3 extends DataVersion {
    public V1451_3() {
        super(1451); // todo what is this version?

        addReference(DataType.ENTITY, "minecraft:egg");
        addReference(DataType.ENTITY, "minecraft:ender_pearl");
        addReference(DataType.ENTITY, "minecraft:fireball");
        addReference(DataType.ENTITY, "minecraft:potion", field -> field
                .single("Potion", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:small_fireball");
        addReference(DataType.ENTITY, "minecraft:snowball");
        addReference(DataType.ENTITY, "minecraft:wither_skull");
        addReference(DataType.ENTITY, "minecraft:xp_bottle");
        addReference(DataType.ENTITY, "minecraft:arrow", field -> field
                .single("inBlockState", DataType.BLOCK_STATE));
        addReference(DataType.ENTITY, "minecraft:enderman", field -> field
                .single("carriedBlockState", DataType.BLOCK_STATE));
        addReference(DataType.ENTITY, "minecraft:falling_block", field -> field
                .single("BlockState", DataType.BLOCK_STATE)
                .single("TileEntityData", DataType.BLOCK_ENTITY));
        addReference(DataType.ENTITY, "minecraft:spectral_arrow", field -> field
                .single("inBlockState", DataType.BLOCK_STATE));
        addReference(DataType.ENTITY, "minecraft:chest_minecart", field -> field
                .single("DisplayState", DataType.BLOCK_STATE)
                .single("LastOutput", DataType.TEXT_COMPONENT));
        addReference(DataType.ENTITY, "minecraft:furnace_minecart", field -> field
                .single("DisplayState", DataType.BLOCK_STATE));
        addReference(DataType.ENTITY, "minecraft:hopper_minecart", field -> field
                .single("DisplayState", DataType.BLOCK_STATE)
                .list("Items", DataType.ITEM_STACK));
        addReference(DataType.ENTITY, "minecraft:minecart", field -> field
                .single("DisplayState", DataType.BLOCK_STATE));
        addReference(DataType.ENTITY, "minecraft:spawner_minecart", field -> field
                .single("DisplayState", DataType.BLOCK_STATE));
        addReference(DataType.ENTITY, "minecraft:tnt_minecart", field -> field
                .single("DisplayState", DataType.BLOCK_STATE));

        // TODO: need to add two more fixes here for flattening
    }
}
