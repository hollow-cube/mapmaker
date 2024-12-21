package net.hollowcube.mapmaker.map.gui.effect.item;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItems;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.gui.effect.item.ItemEditorView.itemSettings;

public class ItemPickerView extends View {
    private static final ItemStack FIREWORK_ITEM = ItemStack.of(Material.FIREWORK_ROCKET);
    private static final ItemStack TRIDENT_ITEM = ItemStack.of(Material.TRIDENT);
    private static final ItemStack AIR_ITEM = ItemStack.of(Material.AIR);

    private @Outlet("title") Text titleText;

    private @Outlet("item_firework_switch") Switch fireworkSwitch;
    private @Outlet("item_trident_switch") Switch tridentSwitch;
    private @Outlet("item_air_switch") Switch airSwitch;

    private final HotbarItems.Mutable items;
    private final int index;

    public ItemPickerView(@NotNull Context context, @NotNull HotbarItems.Mutable items, int index) {
        super(context);
        this.items = items;
        this.index = index;

        titleText.setText("Item Slot #" + (index + 1));

        var item = items.getItem(index);
        if (item != null) {
            fireworkSwitch.setOption(item.material().id() == Material.FIREWORK_ROCKET.id());
            tridentSwitch.setOption(item.material().id() == Material.TRIDENT.id());
            airSwitch.setOption(item.material().id() == Material.AIR.id());
        }
    }

    private void updateItem(@NotNull ItemStack newItem) {
        var existing = items.getItem(index);
        if (existing == null || existing.material().id() != newItem.material().id()) {
            items.setItem(index, newItem);
        }

        var settings = itemSettings(newItem);
        if (settings == null) {
            popView();
            return;
        }

        pushView(settings.getKey());
    }

    @Action("item_firework_off")
    private void fireworkOff() {
        updateItem(FIREWORK_ITEM);
    }

    @Action("item_firework_on")
    private void fireworkOn() {
        updateItem(FIREWORK_ITEM);
    }

    @Action("item_trident_off")
    private void tridentOff() {
        updateItem(TRIDENT_ITEM);
    }

    @Action("item_trident_on")
    private void tridentOn() {
        updateItem(TRIDENT_ITEM);
    }

    @Action("item_air_off")
    private void airOff() {
        updateItem(AIR_ITEM);
    }

    @Action("item_air_on")
    private void airOn() {
        updateItem(AIR_ITEM);
    }
}
