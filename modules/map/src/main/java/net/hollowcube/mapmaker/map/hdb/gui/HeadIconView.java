package net.hollowcube.mapmaker.map.hdb.gui;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.hdb.HeadInfo;
import net.hollowcube.mapmaker.map.util.PlayerUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class HeadIconView extends View {
    private @Outlet("label") Label label;

    private final HeadInfo head;

    public HeadIconView(@NotNull Context context) {
        super(context);
        this.head = null;

        label.setItemSprite(ItemStack.of(Material.BARRIER));
        label.setArgs(Component.text("No Results Found!"));
    }

    public HeadIconView(@NotNull Context context, @NotNull HeadInfo head) {
        super(context);
        this.head = head;

        label.setItemDirect(head.createItemStack());
    }

    @Action("label")
    private void handleSelect(@NotNull Player player) {
        if (head == null) return;

        PlayerUtil.smartAddItemStack(player, head.createItemStack());
        player.closeInventory();
    }
}
