package net.hollowcube.datafix.versions.v0xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V813 extends DataVersion {
    private static final String[] NAMES_BY_COLOR;

    public V813() {
        super(813);

        addFix(DataType.ITEM_STACK, "minecraft:shulker_box", V813::fixItemShulkerBoxColor);
        addFix(DataType.BLOCK_ENTITY, "minecraft:shulker_box", V813::fixBlockEntityShulkerColor);
    }

    private static Value fixItemShulkerBoxColor(Value value) {
        Value tag = value.get("tag");
        if (tag.value() == null) return null;
        Value blockEntityTag = tag.get("BlockEntityTag");
        if (blockEntityTag.value() == null) return null;

        int color = blockEntityTag.get("Color").as(Number.class, 0).intValue();
        blockEntityTag.put("Color", null);

        value.put("id", NAMES_BY_COLOR[color % 16]);
        return null;
    }

    private static Value fixBlockEntityShulkerColor(Value value) {
        value.put("Color", null);
        return null;
    }

    static {
        NAMES_BY_COLOR = new String[]{
                "minecraft:white_shulker_box",
                "minecraft:orange_shulker_box",
                "minecraft:magenta_shulker_box",
                "minecraft:light_blue_shulker_box",
                "minecraft:yellow_shulker_box",
                "minecraft:lime_shulker_box",
                "minecraft:pink_shulker_box",
                "minecraft:gray_shulker_box",
                "minecraft:silver_shulker_box",
                "minecraft:cyan_shulker_box",
                "minecraft:purple_shulker_box",
                "minecraft:blue_shulker_box",
                "minecraft:brown_shulker_box",
                "minecraft:green_shulker_box",
                "minecraft:red_shulker_box",
                "minecraft:black_shulker_box"
        };
    }
}
