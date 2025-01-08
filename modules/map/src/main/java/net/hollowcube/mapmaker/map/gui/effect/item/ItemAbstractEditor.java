package net.hollowcube.mapmaker.map.gui.effect.item;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItem;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItems;
import org.jetbrains.annotations.NotNull;

public class ItemAbstractEditor extends View {
    private final HotbarItems.Mutable items;
    private final int index;

    private HotbarItem item;

    public ItemAbstractEditor(@NotNull Context context, @NotNull HotbarItems.Mutable items, int index) {
        super(context);

        this.items = items;
        this.index = index;

        item = items.getItem(index);
        updateFromState();
    }

    protected void updateItem(@NotNull HotbarItem newItem) {
        items.setItem(index, newItem);
        item = newItem;
        updateFromState();
    }

    protected void updateFromState() {

    }
}
