package net.hollowcube.datafix.versions.v4xxx;

import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.util.Value;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class V4307 extends DataVersion {
    private static final List<String> ADDITIONAL_TOOLTIP_COMPONENTS;

    public V4307() {
        super(4307);

        // TODO this updates the adventure mode predicate item components.
        //  The fix also is one of those writeAndReadTypedOrThrow ones which does *something*

        addFix(DataTypes.DATA_COMPONENTS, V4307::fixConvertToTooltipDisplayComponent);
    }

    private static Value fixConvertToTooltipDisplayComponent(Value dataComponents) {
        var hiddenComponents = Value.emptyList();
        fixGeneric(dataComponents, hiddenComponents, "minecraft:can_place_on", "predicates");
        fixGeneric(dataComponents, hiddenComponents, "minecraft:can_break", "predicates");
        fixGeneric(dataComponents, hiddenComponents, "minecraft:trim", null);
        fixGeneric(dataComponents, hiddenComponents, "minecraft:unbreakable", null);
        fixGeneric(dataComponents, hiddenComponents, "minecraft:dyed_color", "rgb");
        fixGeneric(dataComponents, hiddenComponents, "minecraft:attribute_modifiers", "modifiers");
        fixGeneric(dataComponents, hiddenComponents, "minecraft:enchantments", "levels");
        fixGeneric(dataComponents, hiddenComponents, "minecraft:stored_enchantments", "levels");

        boolean hideTooltip = dataComponents.remove("minecraft:hide_tooltip").as(Boolean.class, false);
        boolean hideAdditionalTooltip = dataComponents.remove("minecraft:hide_additional_tooltip").as(Boolean.class, false);
        if (hideAdditionalTooltip) {
            for (var component : ADDITIONAL_TOOLTIP_COMPONENTS) {
                if (dataComponents.getValue(component) != null) {
                    hiddenComponents.add(component);
                }
            }
        }

        var tooltipDisplay = Value.emptyMap();
        tooltipDisplay.put("hide_tooltip", hideTooltip);
        tooltipDisplay.put("hidden_components", hiddenComponents);
        dataComponents.put("minecraft:tooltip_display", tooltipDisplay);
        return null;
    }

    private static void fixGeneric(Value dataComponents, Value hiddenComponents, String name, @Nullable String field) {
        var component = dataComponents.get(name);

        var showInTooltip = component.remove("show_in_tooltip").as(Boolean.class, true);
        if (!showInTooltip) hiddenComponents.add(name);
        if (field == null) return;

        var flat = component.get(field);
        if (!flat.isNull()) dataComponents.put(name, flat);
    }

    static {
        ADDITIONAL_TOOLTIP_COMPONENTS = List.of(
                "minecraft:banner_patterns",
                "minecraft:bees",
                "minecraft:block_entity_data",
                "minecraft:block_state",
                "minecraft:bundle_contents",
                "minecraft:charged_projectiles",
                "minecraft:container",
                "minecraft:container_loot",
                "minecraft:firework_explosion",
                "minecraft:fireworks",
                "minecraft:instrument",
                "minecraft:map_id",
                "minecraft:painting/variant",
                "minecraft:pot_decorations",
                "minecraft:potion_contents",
                "minecraft:tropical_fish/pattern",
                "minecraft:written_book_content"
        );
    }

}
