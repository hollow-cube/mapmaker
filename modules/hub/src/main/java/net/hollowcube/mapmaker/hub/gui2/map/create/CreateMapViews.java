package net.hollowcube.mapmaker.hub.gui2.map.create;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.View;
import net.hollowcube.canvas.view.ViewContext;
import net.hollowcube.mapmaker.hub.gui2.ExtraViews;
import net.hollowcube.mapmaker.model.MapData;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class CreateMapViews {

    public static @NotNull View MapSlotIcon(@NotNull MapData mapData, int rawSlot) {
        return View.TranslatedButton(Material.GREEN_CONCRETE, "gui.map.slot.icon", List.of(
                Component.text(rawSlot + 1),
                Component.text(mapData.getOwner()),
                Component.text(mapData.getName()),
                Component.text(mapData.getId())
        ), ClickHandler.noop());
    }

    public static @NotNull View MapSlotView(@NotNull ViewContext context, @NotNull MapData mapData, int rawSlot) {
        var pane = View.Pane(9, 6);

        // Header
        pane.add(0, 0, context.create("back", ExtraViews::BackButton));
        pane.add(8, 0, ExtraViews.InfoButton("map.slot"));
        pane.add(4, 0, MapSlotIcon(mapData, rawSlot));

        // Content
        pane.add(3, 2, View.TranslatedButton(Material.DIAMOND_PICKAXE, "gui.map.slot.edit", List.of(), (player, slot, clickType) -> {
            //todo
            return ClickHandler.DENY;
        }));
        pane.add(5, 2, View.TranslatedButton(Material.DIAMOND_BOOTS, "gui.map.slot.verify", List.of(), (player, slot, clickType) -> {
            //todo
            return ClickHandler.DENY;
        }));
        pane.add(2, 3, View.TranslatedButton(Material.ITEM_FRAME, "gui.map.slot.set_icon", List.of(), (player, slot, clickType) -> {
            //todo
            return ClickHandler.DENY;
        }));
        pane.add(4, 3, View.TranslatedButton(Material.ANVIL, "gui.map.slot.set_name", List.of(), (player, slot, clickType) -> {
            //todo
            return ClickHandler.DENY;
        }));
        pane.add(6, 3, View.TranslatedButton(Material.NAME_TAG, "gui.map.slot.set_tags", List.of(), (player, slot, clickType) -> {
            //todo
            return ClickHandler.DENY;
        }));

        return pane;
    }

    public static @NotNull View MapSlotViewLoading(int rawSlot) {
        var pane = View.Pane(9, 6);
        pane.add(0, 0, View.Item(ItemStack.of(Material.PAPER)
                .withDisplayName(Component.text("Loading slot " + (rawSlot + 1) + "..."))));
        return pane;
    }
}
