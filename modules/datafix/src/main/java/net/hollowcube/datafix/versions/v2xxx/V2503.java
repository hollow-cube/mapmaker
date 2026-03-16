package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockStatePropertiesFix;
import net.hollowcube.datafix.util.Value;

import java.util.Set;

public class V2503 extends DataVersion {
    private static final Set<String> WALL_BLOCKS;

    public V2503() {
        super(2503);

        WALL_BLOCKS.forEach(id -> addFix(DataTypes.BLOCK_STATE,
                new BlockStatePropertiesFix(id, V2503::fixWallBlockStates)));
    }

    private static void fixWallBlockStates(Value properties) {
        fixWallProperty(properties, "east");
        fixWallProperty(properties, "west");
        fixWallProperty(properties, "north");
        fixWallProperty(properties, "south");
    }

    private static void fixWallProperty(Value properties, String property) {
        if (!(properties.getValue(property) instanceof String value))
            return;
        properties.put(property, "true".equals(value) ? "low" : "none");
    }

    static {
        WALL_BLOCKS = Set.of(
            "minecraft:andesite_wall",
            "minecraft:brick_wall",
            "minecraft:cobblestone_wall",
            "minecraft:diorite_wall",
            "minecraft:end_stone_brick_wall",
            "minecraft:granite_wall",
            "minecraft:mossy_cobblestone_wall",
            "minecraft:mossy_stone_brick_wall",
            "minecraft:nether_brick_wall",
            "minecraft:prismarine_wall",
            "minecraft:red_nether_brick_wall",
            "minecraft:red_sandstone_wall",
            "minecraft:sandstone_wall",
            "minecraft:stone_brick_wall"
        );
    }
}
