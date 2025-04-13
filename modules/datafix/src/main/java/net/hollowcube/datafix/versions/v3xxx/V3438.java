package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataType;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.BlockEntityRenameFix;
import net.hollowcube.datafix.fixes.ItemRenameFix;
import net.hollowcube.datafix.util.Value;

import java.util.Map;

public class V3438 extends DataVersion {
    private static final Map<String, String> SHERD_RENAMES = Map.of(
            "minecraft:pottery_shard_archer", "minecraft:archer_pottery_shard",
            "minecraft:pottery_shard_prize", "minecraft:prize_pottery_shard",
            "minecraft:pottery_shard_arms_up", "minecraft:arms_up_pottery_shard",
            "minecraft:pottery_shard_skull", "minecraft:skull_pottery_shard"
    );

    public V3438() {
        super(3438);

        renameReference(DataType.BLOCK_ENTITY, "minecraft:suspicious_sand", "minecraft:brushable_block");
        addReference(DataType.BLOCK_ENTITY, "minecraft:calibrated_sculk_sensor");

        addFix(DataType.BLOCK_ENTITY, "minecraft:suspicious_sand", new BlockEntityRenameFix(
                "minecraft:suspicious_sand", "minecraft:brushable_block"));
        addFix(DataType.ENTITY, "minecraft:brushable_block", V3438::fixBrushableBlockEntityLootTableFields);
        addFix(DataType.ITEM_NAME, new ItemRenameFix(SHERD_RENAMES));
    }

    private static Value fixBrushableBlockEntityLootTableFields(Value entity) {
        entity.put("LootTable", entity.remove("loot_table"));
        entity.put("LootTableSeed", entity.remove("loot_table_seed"));
        return null;
    }
}
