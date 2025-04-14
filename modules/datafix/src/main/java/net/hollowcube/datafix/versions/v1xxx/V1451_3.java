package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;

public class V1451_3 extends DataVersion {
    public V1451_3() {
        super(1451, 3);

        addReference(DataTypes.ENTITY, "minecraft:egg");
        addReference(DataTypes.ENTITY, "minecraft:ender_pearl");
        addReference(DataTypes.ENTITY, "minecraft:fireball");
        addReference(DataTypes.ENTITY, "minecraft:potion", field -> field
                .single("Potion", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:small_fireball");
        addReference(DataTypes.ENTITY, "minecraft:snowball");
        addReference(DataTypes.ENTITY, "minecraft:wither_skull");
        addReference(DataTypes.ENTITY, "minecraft:xp_bottle");
        addReference(DataTypes.ENTITY, "minecraft:arrow", field -> field
                .single("inBlockState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:enderman", field -> field
                .single("carriedBlockState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:falling_block", field -> field
                .single("BlockState", DataTypes.BLOCK_STATE)
                .single("TileEntityData", DataTypes.BLOCK_ENTITY));
        addReference(DataTypes.ENTITY, "minecraft:spectral_arrow", field -> field
                .single("inBlockState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:chest_minecart", field -> field
                .single("DisplayState", DataTypes.BLOCK_STATE)
                .single("LastOutput", DataTypes.TEXT_COMPONENT));
        addReference(DataTypes.ENTITY, "minecraft:furnace_minecart", field -> field
                .single("DisplayState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:hopper_minecart", field -> field
                .single("DisplayState", DataTypes.BLOCK_STATE)
                .list("Items", DataTypes.ITEM_STACK));
        addReference(DataTypes.ENTITY, "minecraft:minecart", field -> field
                .single("DisplayState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:spawner_minecart", field -> field
                .single("DisplayState", DataTypes.BLOCK_STATE));
        addReference(DataTypes.ENTITY, "minecraft:tnt_minecart", field -> field
                .single("DisplayState", DataTypes.BLOCK_STATE));

        // TODO: need to add two more fixes here for flattening
    }
}
