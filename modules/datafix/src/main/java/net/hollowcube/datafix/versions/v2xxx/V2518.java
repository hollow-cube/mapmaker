package net.hollowcube.datafix.versions.v2xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockStatePropertiesFix;
import net.hollowcube.datafix.util.Value;

import java.util.Map;

public class V2518 extends DataVersion {
    private static final Map<String, String> ORIENTATIONS_BY_FACING = Map.of(
            "down", "down_south",
            "up", "up_north",
            "north", "north_up",
            "south", "south_up",
            "west", "west_up",
            "east", "east_up"
    );

    public V2518() {
        super(2518);

        addFix(DataType.BLOCK_ENTITY, "minecraft:jigsaw_block", V2518::fixJigsawBlockEntity);
        addFix(DataType.BLOCK_STATE, new BlockStatePropertiesFix("minecraft:jigsaw", V2518::fixJigsawRotation));
    }

    private static Value fixJigsawBlockEntity(Value block) {
        var attachmentType = block.remove("attachement_type").as(String.class, "minecraft:empty");
        var targetPool = block.remove("target_pool").as(String.class, "minecraft:empty");
        block.put("name", attachmentType);
        block.put("target", attachmentType);
        block.put("pool", targetPool);
        return null;
    }

    private static void fixJigsawRotation(Value properties) {
        var facing = properties.remove("facing").as(String.class, "north");
        properties.put("orientation", ORIENTATIONS_BY_FACING.getOrDefault(facing, facing));
    }
}
