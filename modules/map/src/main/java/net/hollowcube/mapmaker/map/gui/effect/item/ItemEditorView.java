package net.hollowcube.mapmaker.map.gui.effect.item;

import net.hollowcube.canvas.*;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.feature.play.effect.HotbarItems;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class ItemEditorView extends View {
    private static final ItemStack PLUS_ITEM = ItemStack.builder(Material.DIAMOND)
            .set(ItemComponent.CUSTOM_MODEL_DATA, BadSprite.require("effect/item/plus").cmd())
            .build();
    private static final ItemStack AIR_ITEM = ItemStack.builder(Material.DIAMOND)
            .set(ItemComponent.CUSTOM_MODEL_DATA, BadSprite.require("effect/item/air").cmd())
            .customName(Component.translatable("gui.effect.item.remove.name"))
            .lore(Component.translatable("gui.effect.item.remove.lore"),
                    Component.text(""), Component.translatable("gui.generic.click_to_select.lore"))
            .build();

    private @Outlet("slots_used") Text slotsUsedText;

    private @OutletGroup("item_\\d+_icon") Label[] itemIcons;
    private @OutletGroup("item_\\d+_opts_switch") Switch[] itemOptsSwitches;
    private @OutletGroup("item_\\d+_opts_enchant") Label[] itemOptButtonsA;
    private @OutletGroup("item_\\d+_opts_settings") Label[] itemOptButtonsB;

    private @Outlet("elytra_cycle_switch") Switch elytraCycleSwitch;
    private @Outlet("elytra_keep_switch") Switch elytraKeepSwitch;
    private @Outlet("elytra_give_switch") Switch elytraGiveSwitch;
    private @Outlet("elytra_take_switch") Switch elytraTakeSwitch;

    private final HotbarItems.Mutable items;

    public ItemEditorView(@NotNull Context context, @NotNull HotbarItems.Mutable items) {
        super(context);
        this.items = items;

        for (int i = 0; i < itemIcons.length; i++) {
            int finalI = i;
            addActionHandler(itemIcons[i].id(), (Label.ActionHandler) (player, slot, clickType) -> {
                if (clickType == ClickType.LEFT_CLICK) {
                    pushTransientView(c -> new ItemPickerView(c, items, finalI));
                } else if (clickType == ClickType.SHIFT_LEFT_CLICK) {
                    items.setItem(finalI, null);
                    updateFromState();
                }
            });
        }

        for (int i = 0; i < itemOptButtonsA.length; i++) {
            int finalI = i;
            addActionHandler(itemOptButtonsA[i].id(), Label.ActionHandler
                    .lmb(ignored -> openItemSettings(finalI)));
        }
        for (int i = 0; i < itemOptButtonsB.length; i++) {
            int finalI = i;
            addActionHandler(itemOptButtonsB[i].id(), Label.ActionHandler
                    .lmb(ignored -> openItemSettings(finalI)));
        }

        updateFromState();
        items.onChange(ignored -> updateFromState());
    }

    @Action("reset")
    private void reset() {
        for (int i = 0; i < 3; i++)
            items.setItem(i, null);
        items.setElytra(null);
        updateFromState();
    }

    @SuppressWarnings("PointlessBooleanExpression")
    private void updateFromState() {
        int count = 0;
        for (int i = 0; i < 3; i++) {
            var item = items.getItem(i);
            if (item == null) {
                itemIcons[i].setItemSprite(PLUS_ITEM);
                itemOptsSwitches[i].setOption(0);
            } else {
                if (item.material().id() == Material.AIR.id()) {
                    itemIcons[i].setItemDirect(AIR_ITEM);
                } else {
                    itemIcons[i].setItemDirect(item);
                }

                var settingsPage = itemSettings(item);
                itemOptsSwitches[i].setOption(settingsPage == null ? 0 : settingsPage.getValue() ? 1 : 2);
                count++;
            }
        }
        slotsUsedText.setText(count + "/3 Slots Used");
        slotsUsedText.setArgs(Component.text(count));

        elytraCycleSwitch.setOption(items.getElytra() == null ? 0 : items.getElytra() != null && items.getElytra() == true ? 1 : 2);
        elytraKeepSwitch.setOption(items.getElytra() == null);
        elytraGiveSwitch.setOption(items.getElytra() != null && items.getElytra() == true);
        elytraTakeSwitch.setOption(items.getElytra() != null && items.getElytra() == false);
    }

    @Action("elytra_cycle_keep")
    private void elytraCycleKeep() {
        setElytraState(true);
    }

    @Action("elytra_cycle_give")
    private void elytraCycleGive() {
        setElytraState(false);
    }

    @Action("elytra_cycle_take")
    private void elytraCycleTake() {
        setElytraState(null);
    }

    @Action("elytra_keep_off")
    private void elytraKeepOff() {
        setElytraState(null);
    }

    @Action("elytra_give_off")
    private void elytraGiveOff() {
        setElytraState(true);
    }

    @Action("elytra_take_off")
    private void elytraTakeOff() {
        setElytraState(false);
    }

    private void setElytraState(@Nullable Boolean newState) {
        items.setElytra(newState);
    }

    private void openItemSettings(int index) {
        var settings = itemSettings(items.getItem(index));
        if (settings == null) return;

        pushView(c -> settings.getKey().create(c, items, index));
    }

    interface ItemSettingsView {
        View create(@NotNull Context context, @NotNull HotbarItems.Mutable items, int index);
    }

    static @Nullable Map.Entry<ItemSettingsView, Boolean> itemSettings(@Nullable ItemStack itemStack) {
        if (itemStack == null) return null;
        var material = itemStack.material();
        if (material.id() == Material.FIREWORK_ROCKET.id()) {
            return Map.entry(ItemFireworkEditor::new, false);
        } else if (material.id() == Material.TRIDENT.id()) {
            return Map.entry(ItemTridentEditor::new, true);
        } else return null;
    }
}
