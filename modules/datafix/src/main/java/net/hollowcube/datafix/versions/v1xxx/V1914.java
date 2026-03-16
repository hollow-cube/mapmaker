package net.hollowcube.datafix.versions.v1xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

public class V1914 extends DataVersion {
    public V1914() {
        super(1914);

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:chest", V1914::fixWeaponSmithChestLootTable);
    }

    private static @Nullable Value fixWeaponSmithChestLootTable(Value value) {
        var lootTable = value.get("LootTable").as(String.class, "");
        if ("minecraft:chests/village_blacksmith".equals(lootTable))
            value.put("LootTable", "minecraft:chests/village/village_weaponsmith");
        return null;
    }
}
