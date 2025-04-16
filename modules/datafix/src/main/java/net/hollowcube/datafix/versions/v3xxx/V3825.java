package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;

import java.util.Set;

public class V3825 extends DataVersion {
    private static final Set<String> MOVED_TAGS = Set.of(
            "spawn_range",
            "total_mobs",
            "simultaneous_mobs",
            "total_mobs_added_per_player",
            "simultaneous_mobs_added_per_player",
            "ticks_between_spawn",
            "spawn_potentials",
            "loot_tables_to_eject",
            "items_to_drop_when_ominous"
    );

    public V3825() {
        super(3825);

        addReference(DataTypes.ENTITY, "minecraft:ominous_item_spawner", field -> field
                .single("item", DataTypes.ITEM_STACK));

        addFix(DataTypes.BLOCK_ENTITY, "minecraft:trial_spawner", V3825::fixMoveToNormalConfig);
    }

    private static Value fixMoveToNormalConfig(Value blockEntity) {
        var normalConfig = Value.emptyMap();
        for (var tag : MOVED_TAGS) normalConfig.put(tag, blockEntity.remove(tag));
        if (normalConfig.size(0) > 0)
            blockEntity.put("normal_config", normalConfig);
        return blockEntity;
    }

}
