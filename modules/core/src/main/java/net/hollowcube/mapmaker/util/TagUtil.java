package net.hollowcube.mapmaker.util;

import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.component.AttributeList;
import net.minestom.server.utils.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class TagUtil {

    public static <T, S> S noop(T t) {
        throw new IllegalStateException("Noop tag writer called");
    }

    public static void removeTooltipExtras(@NotNull ItemStack.Builder item) {
        item.remove(ItemComponent.FIREWORKS); // Has flight duration lore
        item.remove(ItemComponent.ENCHANTMENTS);
        item.remove(ItemComponent.JUKEBOX_PLAYABLE);
        // Note: Just removing attribute modifiers does not remove the armor extras tooltip
        // we actually have to set attributes with nothing for that to happen.
        item.set(ItemComponent.ATTRIBUTE_MODIFIERS, new AttributeList(List.of(), false));
        item.set(ItemComponent.HIDE_ADDITIONAL_TOOLTIP, Unit.INSTANCE);
    }

    private TagUtil() {
    }
}
