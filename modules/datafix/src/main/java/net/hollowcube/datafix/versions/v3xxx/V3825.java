package net.hollowcube.datafix.versions.v3xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public class V3825 extends DataVersion {
    private static final Set<String> MAP_NAMES;
    private static final Set<String> TRIAL_SPAWNER_CONFIG_MOVED_TAGS;
    private static final String OMINOUS_BANNER = "block.minecraft.ominous_banner";

    public V3825() {
        super(3825);

        addReference(DataTypes.ENTITY, "minecraft:ominous_item_spawner", field -> field
            .single("item", DataTypes.ITEM_STACK));

        MAP_NAMES.forEach(id -> addFix(DataTypes.ITEM_STACK, id, V3825::fixItemCustomNameToItemNameMap));
        addFix(DataTypes.ITEM_STACK, "minecraft:white_banner", V3825::fixItemCustomNameToItemNameBanner);
        addFix(DataTypes.BLOCK_ENTITY, "minecraft:banner", V3825::fixOminousBannerBlockEntityName);
        addFix(DataTypes.BLOCK_ENTITY, "minecraft:trial_spawner", V3825::fixMoveToNormalConfig);
    }

    private static @Nullable Value fixItemCustomNameToItemNameBanner(Value itemStack) {
        var components = itemStack.get("components");
        if (!components.isMapLike()) return null;
        var customName = components.get("minecraft:custom_name");
        if (customName.as(String.class, "").contains(OMINOUS_BANNER)) {
            components.put("minecraft:item_name", customName);
            components.remove("minecraft:custom_name");
        }
        return null;
    }

    private static @Nullable Value fixItemCustomNameToItemNameMap(Value itemStack) {
        var components = itemStack.get("components");
        if (!components.isMapLike()) return null;
        var customName = components.get("minecraft:custom_name");
        var customNameString = customName.as(String.class, "");
        if (MAP_NAMES.stream().anyMatch(name -> name.contains(customNameString))) {
            components.put("minecraft:item_name", customName);
            components.remove("minecraft:custom_name");
        }
        return null;
    }

    private static @Nullable Value fixOminousBannerBlockEntityName(Value blockEntity) {
        var customName = blockEntity.get("CustomName");
        if (!customName.as(String.class, "").contains(OMINOUS_BANNER))
            return null;

        blockEntity.remove("CustomName");
        var components = blockEntity.get("components", Value::emptyMap);
        components.put("minecraft:item_name", customName);
        components.put("minecraft:hide_additional_tooltip", Value.emptyMap());
        blockEntity.put("components", components);

        return null;
    }

    private static Value fixMoveToNormalConfig(Value blockEntity) {
        var normalConfig = Value.emptyMap();
        for (var tag : TRIAL_SPAWNER_CONFIG_MOVED_TAGS) normalConfig.put(tag, blockEntity.remove(tag));
        if (normalConfig.size(0) > 0)
            blockEntity.put("normal_config", normalConfig);
        return blockEntity;
    }

    static {
        MAP_NAMES = Set.of(
            "filled_map.buried_treasure",
            "filled_map.explorer_jungle",
            "filled_map.explorer_swamp",
            "filled_map.mansion",
            "filled_map.monument",
            "filled_map.trial_chambers",
            "filled_map.village_desert",
            "filled_map.village_plains",
            "filled_map.village_savanna",
            "filled_map.village_snowy",
            "filled_map.village_taiga"
        );
        TRIAL_SPAWNER_CONFIG_MOVED_TAGS = Set.of(
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
    }

}
