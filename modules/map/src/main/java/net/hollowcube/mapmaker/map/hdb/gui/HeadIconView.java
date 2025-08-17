package net.hollowcube.mapmaker.map.hdb.gui;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.PlayerUtil;
import net.hollowcube.mapmaker.map.hdb.HeadInfo;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class HeadIconView extends View {
    private @Outlet("label") Label label;

    private final HeadInfo head;
    private final String alphabetSubcategory;

    public HeadIconView(@NotNull Context context) {
        super(context);
        this.head = null;
        this.alphabetSubcategory = null;

        label.setItemSprite(ItemStack.of(Material.BARRIER));
        label.setArgs(Component.text("No Results Found!"));
    }

    public HeadIconView(@NotNull Context context, @NotNull HeadInfo head) {
        super(context);
        this.head = head;
        this.alphabetSubcategory = "__alphabet".equals(head.category()) ? head.name() : null;

        label.setItemDirect(head.createItemStack());
    }

    @Action("label")
    private void handleSelect(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        if (head == null || (clickType != ClickType.LEFT_CLICK && clickType != ClickType.SHIFT_LEFT_CLICK)) return;

        // Gross special case for alphabet subcategory
        if (alphabetSubcategory != null) {
            performSignal(CategoryIconView.SIG_SELECTED, "alphabet|" + alphabetSubcategory);
            return;
        }

        PlayerUtil.giveItem(player, head.createItemStack());
        if (clickType == ClickType.LEFT_CLICK) player.closeInventory();
    }
}
