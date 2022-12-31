package net.hollowcube.mapmaker.hub.gui.map;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.Section;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.common.lang.LanguageProvider;
import net.hollowcube.mapmaker.hub.gui.map.component.MapSlotButton;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.storage.MapStorage;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class MapSlotsView extends ParentSection {
    private final PlayerData playerData;

    public MapSlotsView(@NotNull PlayerData playerData) {
        super(5, 1);
        this.playerData = playerData;
    }

    @Override
    protected void mount() {
        super.mount();

        // Re/load view each time it is shown
        asyncLoadMapSlots();
    }

    private void asyncLoadMapSlots() {
        for (int i = 0; i < PlayerData.MAX_MAP_SLOTS; i++) {
            var slot = i + 1;
            var last = get(i);
            if (i >= playerData.getUnlockedMapSlots()) {
                // Add the button if missing or not a LockedButton. It would be missing the first time
                // the GUI is opened, and it would not be a LockedButton if the number of slots the player
                // has unlocked is less than it was the first time the GUI was opened.
                if (!(last instanceof LockedButton)) {
                    if (last != null) unmountChild(i, last);
                    add(i, createLockedSlotButton(slot));
                }
                continue;
            }

            // Fetch each data slot. If there is already an item in the slot then we do nothing (indicates that
            // we are reloading). If there is no item it means this is the first load so add the button immediately.
            // todo we should add a little loading spinner in the corner of the GUI in case reloading takes more
            //  than half a second or something
            var mapId = playerData.getMapSlot(i);
            if (mapId != null) {
                var mapFuture = getContext(MapStorage.class).getMapById(mapId);
                var newButton = new MapSlotButton(slot, mapFuture, () -> {
                    var router = find(RouterSection.class);
                    router.push(new MapSlotView(slot, mapFuture));
                });

                var idx = i;
                if (last == null) {
                    add(i, newButton);
                } else {
                    mapFuture.then(unused -> {
                        unmountChild(idx, last);
                        add(idx, newButton);
                    }).thenErr(err -> {
                        unmountChild(idx, last);
                        add(idx, newButton);

                        //todo log error
                    });
                }
            } else {
                if (last != null) unmountChild(i, last);
                add(i, createEmptySlotIcon(slot));
            }
        }
    }

    private @NotNull Section createLockedSlotButton(int slot) {
        var item = ItemStack.builder(Material.BEDROCK)
                .amount(slot)
                .displayName(Component.translatable("gui.map_slot.locked.name", Component.text(slot)))
                .lore(LanguageProvider.createMultiTranslatable("gui.map_slot.locked.lore", Component.text(slot)))
                .build();
        return new LockedButton(1, 1, item, () -> {/* todo */});
    }

    private @NotNull Section createEmptySlotIcon(int slot) {
        var item = ItemStack.builder(Material.RED_CONCRETE)
                .amount(slot)
                .displayName(Component.translatable("gui.map_slot.empty.name", Component.text(slot)))
                .lore(LanguageProvider.createMultiTranslatable("gui.map_slot.empty.lore", Component.text(slot)))
                .build();
        return new ButtonSection(1, 1, item, () -> {
            var router = find(RouterSection.class);
            router.pushTransient(new CreateMapView(slot));
        });
    }

    // This class only exists to use as a marker for a locked slot, see comment in asyncLoadMapSlots
    private static class LockedButton extends ButtonSection {

        public LockedButton(int width, int height, @NotNull ItemStack item) {
            super(width, height, item);
        }

        public LockedButton(int width, int height, @NotNull ItemStack item, @Nullable Runnable onClick) {
            super(width, height, item, onClick);
        }

        public LockedButton(int width, int height, @NotNull ItemStack item, @Nullable ClickHandler onClick) {
            super(width, height, item, onClick);
        }
    }

}
