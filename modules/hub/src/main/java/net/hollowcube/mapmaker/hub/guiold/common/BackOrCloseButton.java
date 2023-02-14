package net.hollowcube.mapmaker.hub.guiold.common;

import net.hollowcube.canvas.section.ClickHandler;
import net.hollowcube.canvas.section.RouterSection;
import net.hollowcube.canvas.section.std.ButtonSection;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;

public class BackOrCloseButton extends ButtonSection {

    private static final ItemStack BACK_ITEM = ItemStack.builder(Material.ARROW)
            .displayName(Component.translatable("gui.generic.back.name"))
            .build();

    private static final ItemStack CLOSE_ITEM = ItemStack.builder(Material.STRUCTURE_VOID)
            .displayName(Component.translatable("gui.generic.close.name"))
            .build();

    public BackOrCloseButton() {
        super(1, 1, ItemStack.AIR);
    }

    @Override
    protected void mount() {
        super.mount();

        var router = find(RouterSection.class);
        if (router.hasHistory()) {
            setItem(BACK_ITEM);
            setOnClick(router::pop);
        } else {
            setItem(CLOSE_ITEM);
            setClickHandler((player, slot, clickType) -> {
                player.closeInventory();
                return ClickHandler.DENY;
            });
        }
    }

}
