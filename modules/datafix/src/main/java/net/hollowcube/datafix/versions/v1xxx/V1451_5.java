package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

import java.util.Set;

public class V1451_5 extends DataVersion {
    private static final Set<String> ITEMS;

    public V1451_5() {
        super(1451, 5);

        removeReference(DataTypes.BLOCK_ENTITY, "minecraft:flower_pot");
        removeReference(DataTypes.BLOCK_ENTITY, "minecraft:noteblock");

        ITEMS.forEach(id -> addFix(DataTypes.ITEM_STACK, id, V1451_5::fixRemoveBlockEntityTag));
    }

    private static Value fixRemoveBlockEntityTag(Value itemStack) {
        // TODO: I (matt) have no idea what this does in any way shape or form.
        return null;
    }

    static {
        ITEMS = Set.of(
                "minecraft:noteblock",
                "minecraft:flower_pot",
                "minecraft:dandelion",
                "minecraft:poppy",
                "minecraft:blue_orchid",
                "minecraft:allium",
                "minecraft:azure_bluet",
                "minecraft:red_tulip",
                "minecraft:orange_tulip",
                "minecraft:white_tulip",
                "minecraft:pink_tulip",
                "minecraft:oxeye_daisy",
                "minecraft:cactus",
                "minecraft:brown_mushroom",
                "minecraft:red_mushroom",
                "minecraft:oak_sapling",
                "minecraft:spruce_sapling",
                "minecraft:birch_sapling",
                "minecraft:jungle_sapling",
                "minecraft:acacia_sapling",
                "minecraft:dark_oak_sapling",
                "minecraft:dead_bush",
                "minecraft:fern"
        );
    }
}
