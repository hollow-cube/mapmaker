package net.hollowcube.mapmaker.hub.gui.section;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.Section;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.canvas.std.GroupSection;
import net.hollowcube.mapmaker.lang.LanguageProvider;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.hollowcube.mapmaker.result.Error;
import net.hollowcube.mapmaker.result.FutureResult;
import net.hollowcube.mapmaker.result.Result;
import net.hollowcube.mapmaker.util.StaticAbuse;
import net.kyori.adventure.text.Component;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.locks.ReentrantLock;

public class MapSlotsSection extends ParentSection {

    public MapSlotsSection(@NotNull PlayerData playerData) {
        super(5, 1);

        for (int i = 0; i < PlayerData.MAX_MAP_SLOTS; i++) {
            if (i < playerData.getUnlockedMapSlots()) {
                var mapId = playerData.getMapSlot(i);
                //todo should allow clicking into a map info while it is loading, but should NOT
                // be allowed to click once it has become an error.
                if (mapId != null) {
                    add(i, new MapSlotMapSection(i, mapId));
                } else {
                    add(i, createEmptySlotIcon(i));
                }
            } else {
                add(i, createLockedSlotButton(i));
            }
        }
    }

    private @NotNull Section createLockedSlotButton(int index) {
        var item = ItemStack.builder(Material.BEDROCK)
                .amount(index + 1)
                .displayName(Component.translatable("gui.map_slot.locked.name", Component.text(index + 1)))
                .lore(LanguageProvider.createMultiTranslatable("gui.map_slot.locked.lore", Component.text(index + 1)))
                .build();
        return new ButtonSection(1, 1, item, () -> {/* todo */});
    }

    private @NotNull Section createEmptySlotIcon(int index) {
        var item = ItemStack.builder(Material.RED_CONCRETE)
                .amount(index + 1)
                .displayName(Component.translatable("gui.map_slot.empty.name", Component.text(index + 1)))
                .lore(LanguageProvider.createMultiTranslatable("gui.map_slot.empty.lore", Component.text(index + 1)))
                .build();
        return new ButtonSection(1, 1, item, () -> {/* todo */});
    }

    /**
     * Represents a map slot that is filled, handling the loading time of the map info.
     */
    private static class MapSlotMapSection extends ButtonSection {
        private static final ItemStack LOADING_ICON = ItemStack.builder(Material.GREEN_CONCRETE)
                .displayName(Component.translatable("gui.map_slot.loading.name"))
                .build();

        //todo need to handle clicking while loading, eg before MapData is set.

        private final String mapId;

        private FutureResult<MapData> mapFuture;
        // Access to this field is not synchronized. It only controls the loading/error/success icon
        // so if there is a race for setting it does not matter.
        private Result<MapData> map;

        public MapSlotMapSection(int index, @NotNull String mapId) {
            super(1, 1, LOADING_ICON);
            this.mapId = mapId;

            setOnClick(this::handleClick);
        }

        @Override
        protected void mount() {
            super.mount();

            if (mapFuture != null) return; // already loaded

            mapFuture = StaticAbuse.mapStorage.getMapById(mapId)
                    .map(mapData -> {
                        loadSuccess(mapData);
                        return mapData;
                    })
                    .mapErr(err -> {
                        loadError(err);
                        return Result.error(err);
                    });
        }

        private void handleClick() {
            // If map is error, do not allow click.
            if (map != null && map.isErr()) return;
            Check.notNull(mapFuture, "mapFuture"); // Sanity check

            // Map is either loading or loaded, open the next gui
            //todo temp
            var next = new GroupSection(9, 3);
            next.add(0, 0, new ButtonSection(1, 1, ItemStack.builder(Material.RED_CONCRETE).build(), () -> {
                //todo temp
                find(RouterSection.class).pop();
            }));
            find(RouterSection.class).push(next);
        }

        private void loadSuccess(@NotNull MapData mapData) {
            map = Result.of(mapData);
            setItem(ItemStack.builder(Material.GREEN_CONCRETE)
                    .displayName(Component.text(mapData.getName()))
                    .build());
        }

        private void loadError(@NotNull Error err) {
            map = Result.error(err);
            setItem(ItemStack.builder(Material.BARRIER)
                    .displayName(Component.text(err.message()))
                    .build());
        }
    }

}
