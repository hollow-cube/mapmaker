package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

public class V4181 extends DataVersion {
    public V4181() {
        super(4181);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:furnace", V4181::fixFurnaceLikeCookTime);
        addFix(DataTypes.BLOCK_ENTITY, "minecraft:smoker", V4181::fixFurnaceLikeCookTime);
        addFix(DataTypes.BLOCK_ENTITY, "minecraft:blast_furnace", V4181::fixFurnaceLikeCookTime);
    }

    private static Value fixFurnaceLikeCookTime(Value blockEntity) {
        blockEntity.put("cooking_time_spent", blockEntity.remove("CookTime"));
        blockEntity.put("cooking_total_time", blockEntity.remove("CookTimeTotal"));
        blockEntity.put("lit_time_remaining", blockEntity.remove("BurnTime"));
        blockEntity.put("lit_total_time", blockEntity.get("lit_time_remaining"));
        return null;
    }
}
