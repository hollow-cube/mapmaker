package net.hollowcube.mapmaker.hub.gui.common;

import net.hollowcube.canvas.section.ClickHandler;
import net.hollowcube.canvas.section.RouterSection;
import net.hollowcube.canvas.section.std.ButtonSection;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.Enchantment;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

public class IconSelection extends ButtonSection {

    private final List<Material> icons = List.of(
            Material.OAK_PLANKS,
            Material.APPLE
    );
    private int shinyIndex = -1;
    private final Consumer<ItemStack> saveCallback;

    public IconSelection(@Nullable ItemStack current, @NotNull Consumer<ItemStack> callback) {
        super(9, 3, ItemStack.of(Material.AIR));
        if (current != null) {
            for (int i = 0; i < icons.size() && i < width() * height(); i++) {
                if (current.material() == icons.get(i)) {
                    shinyIndex = i;
                    break;
                }
            }
        }
        this.saveCallback = callback;
    }

    @Override
    protected void mount() {
        super.mount();
        for (int i = 0; i < icons.size() && i < width() * height(); i++) {
            if (shinyIndex == i) {
                // Make the currently selected one shiny
                setItem(i, ItemStack.of(icons.get(i)).withMeta(meta -> meta.enchantment(Enchantment.VANISHING_CURSE, (short) 1)));
            } else {
                setItem(i, ItemStack.of(icons.get(i)));
            }
        }

        setClickHandler((player, slot, clickType) -> {
            if (clickType == ClickType.LEFT_CLICK) {
                if (getItem(slot).isAir()) return ClickHandler.DENY;

                var router = find(RouterSection.class);
                router.pop();
                // Has to go after router.pop();
                saveCallback.accept(getItem(slot));
            }
            return ClickHandler.DENY;
        });
    }
}
