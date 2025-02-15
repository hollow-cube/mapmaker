package net.hollowcube.mapmaker.util;

import net.hollowcube.common.util.OpUtils;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.item.component.AttributeList;
import net.minestom.server.utils.NamespaceID;
import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class ItemUtils {

    private static final Set<NamespaceID> USE_MODEL_OVERRIDE = OpUtils.build(new HashSet<>(), set -> {
        for (var value : Material.values()) {
            String path = value.namespace().path();
            if (path.endsWith("_smithing_template")) {
                set.add(value.namespace());
            }
        }
    });

    private ItemUtils() {
    }

    public static @NotNull Component translation(@NotNull Material material) {
        var namespace = material.namespace();
        String prefix = material.isBlock() ? "block" : "item";
        var translationKey = String.format("%s.%s.%s", prefix, namespace.namespace(), namespace.path());
        return Component.translatable(translationKey);
    }

    @SuppressWarnings("UnstableApiUsage")
    // In 1.21.5 all of these checks can be removed and directly use the item definition as special renderers are not hardcoded anymore and colors are defined in definitions not a global map
    public static ItemStack asDisplay(@NotNull Material material) {
        var id = material.namespace();
        if (USE_MODEL_OVERRIDE.contains(id)) {
            String model = material.prototype().get(ItemComponent.ITEM_MODEL);
            return ItemStack.builder(Material.STICK)
                    .set(ItemComponent.ITEM_MODEL, model)
                    .build();
        }
        return ItemStack.builder(material)
                .remove(ItemComponent.FIREWORKS) // Has flight duration lore
                .remove(ItemComponent.ENCHANTMENTS)
                .remove(ItemComponent.JUKEBOX_PLAYABLE)
                .remove(ItemComponent.OMINOUS_BOTTLE_AMPLIFIER)
                // Note: Just removing attribute modifiers does not remove the armor extras tooltip
                // we actually have to set attributes with nothing for that to happen.
                .set(ItemComponent.ATTRIBUTE_MODIFIERS, new AttributeList(List.of(), false))
                .set(ItemComponent.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE)
                .build();
    }
}
