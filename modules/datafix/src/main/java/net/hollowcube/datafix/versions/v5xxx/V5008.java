package net.hollowcube.datafix.versions.v5xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

import static net.hollowcube.datafix.util.DataFixUtils.namespaced;

public class V5008 extends DataVersion {
    private record ExplorerMap(String itemId, String legacyNameKey) {
    }

    private static final Map<String, ExplorerMap> EXPLORER_MAPS = Map.ofEntries(
            Map.entry("minecraft:mansion", new ExplorerMap("minecraft:woodland_explorer_map", "filled_map.mansion")),
            Map.entry("minecraft:monument", new ExplorerMap("minecraft:ocean_explorer_map", "filled_map.monument")),
            Map.entry("minecraft:trial_chambers", new ExplorerMap("minecraft:trial_explorer_map", "filled_map.trial_chambers")),
            Map.entry("minecraft:jungle_temple", new ExplorerMap("minecraft:jungle_explorer_map", "filled_map.explorer_jungle")),
            Map.entry("minecraft:swamp_hut", new ExplorerMap("minecraft:swamp_explorer_map", "filled_map.explorer_swamp")),
            Map.entry("minecraft:village_desert", new ExplorerMap("minecraft:desert_village_map", "filled_map.village_desert")),
            Map.entry("minecraft:village_plains", new ExplorerMap("minecraft:plains_village_map", "filled_map.village_plains")),
            Map.entry("minecraft:village_savanna", new ExplorerMap("minecraft:savanna_village_map", "filled_map.village_savanna")),
            Map.entry("minecraft:village_snowy", new ExplorerMap("minecraft:snowy_village_map", "filled_map.village_snowy")),
            Map.entry("minecraft:village_taiga", new ExplorerMap("minecraft:taiga_village_map", "filled_map.village_taiga")),
            Map.entry("minecraft:red_x", new ExplorerMap("minecraft:buried_treasure_map", "filled_map.buried_treasure"))
    );

    public V5008() {
        super(5008);

        addFix(DataTypes.ITEM_STACK, "minecraft:filled_map", V5008::fixExplorerMapItem);
        addFix(DataTypes.DATA_COMPONENTS, V5008::fixRemoveMapColorComponent);
    }

    private static Value fixExplorerMapItem(Value itemStack) {
        var components = itemStack.get("components");
        // The explorer decoration is always stored under the "+" key by vanilla.
        var explorerDecoration = components.get("minecraft:map_decorations").get("+");
        if (!explorerDecoration.isMapLike()) return null;

        var decorationType = namespaced(explorerDecoration.get("type").as(String.class, ""));
        var explorerMap = EXPLORER_MAPS.get(decorationType);
        if (explorerMap == null) return null;

        itemStack.put("id", explorerMap.itemId());
        if (explorerMap.legacyNameKey().equals(getPlainTranslationKey(components.get("minecraft:item_name"))))
            components.remove("minecraft:item_name");
        return null;
    }

    private static Value fixRemoveMapColorComponent(Value dataComponents) {
        dataComponents.remove("minecraft:map_color");
        return null;
    }

    static @Nullable String getPlainTranslationKey(Value textComponent) {
        if (!textComponent.isMapLike() || textComponent.size(0) != 1) return null;
        return textComponent.get("translate").as(String.class, null);
    }

}
