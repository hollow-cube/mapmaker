package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.fixes.ItemRenameFix;

import java.util.Map;

public class V3447 extends DataVersion {
    private static final Map<String, String> RENAMES;

    public V3447() {
        super(3447);

        addFix(DataTypes.ITEM_NAME, new ItemRenameFix(RENAMES));
    }

    static {
        RENAMES = Map.ofEntries(
            Map.entry("minecraft:angler_pottery_shard", "minecraft:angler_pottery_sherd"),
            Map.entry("minecraft:archer_pottery_shard", "minecraft:archer_pottery_sherd"),
            Map.entry("minecraft:arms_up_pottery_shard", "minecraft:arms_up_pottery_sherd"),
            Map.entry("minecraft:blade_pottery_shard", "minecraft:blade_pottery_sherd"),
            Map.entry("minecraft:brewer_pottery_shard", "minecraft:brewer_pottery_sherd"),
            Map.entry("minecraft:burn_pottery_shard", "minecraft:burn_pottery_sherd"),
            Map.entry("minecraft:danger_pottery_shard", "minecraft:danger_pottery_sherd"),
            Map.entry("minecraft:explorer_pottery_shard", "minecraft:explorer_pottery_sherd"),
            Map.entry("minecraft:friend_pottery_shard", "minecraft:friend_pottery_sherd"),
            Map.entry("minecraft:heart_pottery_shard", "minecraft:heart_pottery_sherd"),
            Map.entry("minecraft:heartbreak_pottery_shard", "minecraft:heartbreak_pottery_sherd"),
            Map.entry("minecraft:howl_pottery_shard", "minecraft:howl_pottery_sherd"),
            Map.entry("minecraft:miner_pottery_shard", "minecraft:miner_pottery_sherd"),
            Map.entry("minecraft:mourner_pottery_shard", "minecraft:mourner_pottery_sherd"),
            Map.entry("minecraft:plenty_pottery_shard", "minecraft:plenty_pottery_sherd"),
            Map.entry("minecraft:prize_pottery_shard", "minecraft:prize_pottery_sherd"),
            Map.entry("minecraft:sheaf_pottery_shard", "minecraft:sheaf_pottery_sherd"),
            Map.entry("minecraft:shelter_pottery_shard", "minecraft:shelter_pottery_sherd"),
            Map.entry("minecraft:skull_pottery_shard", "minecraft:skull_pottery_sherd"),
            Map.entry("minecraft:snort_pottery_shard", "minecraft:snort_pottery_sherd")
        );
    }
}
