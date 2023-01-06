package net.hollowcube.mapmaker.hub.gui2.map;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.view.View;
import net.hollowcube.canvas.view.ViewContext;
import net.hollowcube.mapmaker.hub.gui2.ExtraViews;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class CreateMapViews {
    private CreateMapViews() {
    }

    public static @NotNull View CreateMapsView(@NotNull ViewContext context) {
        var playerData = PlayerData.fromPlayer(context.player());
        var pane = View.Pane(9, 3);
        context.setTitle(Component.text("\uF808\uEff2", NamedTextColor.WHITE)); //todo temp

        // Common buttons
        pane.add(0, 0, context.create("back", ExtraViews::BackButton));
        pane.add(8, 0, ExtraViews.InfoButton("create_maps"));

        // Map selector
        pane.add(0, 1, context.create("slots", c -> MapSlotsView(c, playerData)));

        return pane;
    }

    public static @NotNull View MapSlotsView(@NotNull ViewContext context, @NotNull PlayerData playerData) {
        var pane = View.Pane(5, 1);
        for (int i = 0; i < PlayerData.MAX_MAP_SLOTS; i++) {

            // Any slot over the unlocked slots is a locked button
            if (i >= playerData.getUnlockedMapSlots()) {
                pane.add(i, 0, LockedSlotButton(i));
                continue;
            }

            // Handle empty slots
            var mapId = playerData.getMapSlot(i);
            if (mapId == null) {
                pane.add(i, 0, EmptySlotButton(i));
                continue;
            }

            // Filled slot, create a loading indicator
            var slot = i;
            pane.add(i, 0, context.create("slot_" + i, c -> LoadableMapSlotButton(c, slot, mapId)));
        }
        return pane;
    }

    public static @NotNull View LoadableMapSlotButton(@NotNull ViewContext context, int rawSlot, @NotNull String mapId) {
        var mapFuture = context.get("map", () -> {
            return context.env(MapStorage.class).getMapById(mapId);
        });
        var openMap = ClickHandler.leftClick(() -> context.pushView(9, 6, c ->
                MapSlotViews.LoadableSlotView(c, rawSlot, mapFuture)));
        return context.create("loading", c1 -> View.Loading(c1, mapFuture,
                c -> LoadingSlotButton(rawSlot, openMap),
                c -> LoadedSlotButton(rawSlot, mapFuture.await().result(), openMap),
                c -> ExtraViews.Error(9, 6, mapFuture.await().error())));
    }

    public static @NotNull View LoadedSlotButton(int rawSlot, @NotNull MapData mapData, @NotNull ClickHandler clickHandler) {
        var translationArgs = List.<Component>of(
                Component.text(rawSlot + 1),
                Component.text(mapData.getOwner()),
                Component.text(mapData.getName()),
                Component.text(mapData.getId())
        );
        return View.TranslatedButton(ItemStack.of(Material.GREEN_CONCRETE, rawSlot + 1),
                "gui.create_maps.loaded_slot", translationArgs, clickHandler);
    }

    public static @NotNull View LoadingSlotButton(int rawSlot, @NotNull ClickHandler clickHandler) {
        return View.TranslatedButton(ItemStack.of(Material.GREEN_CONCRETE, rawSlot + 1),
                "gui.create_maps.loading_slot", List.of(Component.text(rawSlot + 1)),
                clickHandler);
    }

    public static @NotNull View LockedSlotButton(int rawSlot) {
        var slot = rawSlot + 1;
        //todo should open store prompt
        return View.TranslatedButton(ItemStack.of(Material.BEDROCK, slot),
                "gui.create_maps.locked_slot", List.of(Component.text(slot)),
                ClickHandler.noop());
    }

    public static @NotNull View EmptySlotButton(int rawSlot) {
        var slot = rawSlot + 1;
        return View.TranslatedButton(ItemStack.of(Material.RED_CONCRETE, slot),
                "gui.create_maps.empty_slot", List.of(Component.text(slot)),
                ClickHandler.leftClick(() -> {
                    //todo
                }));
    }

}
