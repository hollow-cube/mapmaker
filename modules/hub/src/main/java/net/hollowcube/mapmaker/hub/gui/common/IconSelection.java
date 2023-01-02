package net.hollowcube.mapmaker.hub.gui.common;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.std.ButtonSection;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.Enchantment;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Consumer;

public class IconSelection extends ButtonSection {

    private final List<Material> icons = List.of(
            Material.OAK_PLANKS
    );
    private int shinyIndex = -1;

    public IconSelection(@NotNull ItemStack current, @NotNull Consumer<ItemStack> callback) {
        super(9, 3, ItemStack.of(Material.AIR));


        for (int i = 0; i < icons.size() && i < width() * height(); i++) {
            if (current.material() == icons.get(i)) {
                // Make the currently selected one shiny
                setItem(i, ItemStack.of(icons.get(i)).withMeta(meta -> meta.enchantment(Enchantment.VANISHING_CURSE, (short) 1)));
                shinyIndex = i;
            } else {
                setItem(i, ItemStack.of(icons.get(i)));
            }
        }

        setClickHandler((player, slot, clickType) -> {
            if (clickType == ClickType.LEFT_CLICK || clickType == ClickType.SHIFT_CLICK) {
                if (getItem(slot).isAir()) return ClickHandler.DENY;
                
                callback.accept(getItem(slot));
                // Update shiny item
                if (shinyIndex != -1) {
                    setItem(shinyIndex, ItemStack.of(icons.get(shinyIndex)));
                }
                setItem(slot, ItemStack.of(icons.get(slot)));
                shinyIndex = slot;
                return ClickHandler.ALLOW;
            } else {
                return ClickHandler.DENY;
            }
        });
    }
}
