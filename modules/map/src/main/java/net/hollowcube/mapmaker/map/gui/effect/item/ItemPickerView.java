package net.hollowcube.mapmaker.map.gui.effect.item;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItem;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItems;
import net.hollowcube.mapmaker.map.item.vanilla.FireworkRocketItem;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.mapmaker.map.gui.effect.item.ItemEditorView.itemSettings;

public class ItemPickerView extends View {
    private static final ItemStack FIREWORK_ITEM = HotbarItem.FireworkRocket.DEFAULT.toItemStack(true);
    private static final ItemStack TRIDENT_ITEM = HotbarItem.Trident.DEFAULT.toItemStack(true);

    private @Outlet("title") Text titleText;
    private @Outlet("item_slot") Text itemSlot;

    private @Outlet("item_firework_off") Label fireworkOffLabel;
    private @Outlet("item_firework_on") Label fireworkOnLabel;
    private @Outlet("item_trident_off") Label tridentOffLabel;
    private @Outlet("item_trident_on") Label tridentOnLabel;

    private @Outlet("item_firework_switch") Switch fireworkSwitch;
    private @Outlet("item_trident_switch") Switch tridentSwitch;
    private @Outlet("item_air_switch") Switch airSwitch;

    private final HotbarItems.Mutable items;
    private final int index;

    public ItemPickerView(@NotNull Context context, @NotNull HotbarItems.Mutable items, int index) {
        super(context);
        this.items = items;
        this.index = index;

        titleText.setText("Choose Item");
        itemSlot.setText("Item Slot #" + (index + 1));
        itemSlot.setArgs(Component.text(index + 1));
        fireworkOffLabel.setItemSprite(FIREWORK_ITEM);
        fireworkOnLabel.setItemSprite(FIREWORK_ITEM);
        tridentOffLabel.setItemSprite(TRIDENT_ITEM);
        tridentOnLabel.setItemSprite(TRIDENT_ITEM);

        var item = items.getItem(index);
        if (item != null) {
            fireworkSwitch.setOption(item instanceof HotbarItem.FireworkRocket);
            tridentSwitch.setOption(item instanceof HotbarItem.Trident);
            airSwitch.setOption(item instanceof HotbarItem.Remove);
        }
    }

    private void updateItem(@NotNull HotbarItem newItem) {
        var existing = items.getItem(index);
        if (existing == null || !existing.name().equals(newItem.name())) {
            items.setItem(index, newItem);
        }

        var settings = itemSettings(newItem);
        if (settings == null) {
            popView();
            return;
        }

        pushView(c -> settings.getKey().create(c, items, index));
    }

    @Action("item_firework_off")
    private void fireworkOff() {
        updateItem(HotbarItem.FireworkRocket.DEFAULT);
    }

    @Action("item_firework_on")
    private void fireworkOn() {
        updateItem(HotbarItem.FireworkRocket.DEFAULT);
    }

    @Action("item_trident_off")
    private void tridentOff() {
        updateItem(HotbarItem.Trident.DEFAULT);
    }

    @Action("item_trident_on")
    private void tridentOn() {
        updateItem(HotbarItem.Trident.DEFAULT);
    }

    @Action("item_air_off")
    private void airOff() {
        updateItem(HotbarItem.Remove.INSTANCE);
    }

    @Action("item_air_on")
    private void airOn() {
        updateItem(HotbarItem.Remove.INSTANCE);
    }

    @Action("reset")
    private void reset() {
        items.setItem(index, null);
        popView();
    }
}
