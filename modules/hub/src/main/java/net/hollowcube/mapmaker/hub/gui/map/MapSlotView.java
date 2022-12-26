package net.hollowcube.mapmaker.hub.gui.map;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RootSection;
import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.common.result.Error;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.hub.Handler;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.hub.gui.common.BackOrCloseButton;
import net.hollowcube.mapmaker.hub.gui.common.GenericNameInput;
import net.hollowcube.mapmaker.hub.gui.common.TranslatedButtonSection;
import net.hollowcube.mapmaker.hub.gui.map.component.MapSlotButton;
import net.hollowcube.mapmaker.model.MapData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class MapSlotView extends ParentSection {
    private static final Logger logger = LoggerFactory.getLogger(MapSlotView.class);

    private final ButtonSection loadingButton;

    private final int rawSlot;
    private MapData map;
    private ActionView inner = null;

    public MapSlotView(int rawSlot, @Nullable FutureResult<MapData> mapFuture) {
        super(9, 6);
        this.rawSlot = rawSlot;

        //todo for gui lib
        // better define removing something. Should be able to remove by any single one of its indices,
        // and should handle a component overlapping by throwing an exception.

        // Add loading button
        loadingButton = add(4, 2, new ButtonSection(1, 1, MapSlotButton.LOADING_ICON));

        if (mapFuture == null) {
            throw new RuntimeException("not implemented");
        }

        mapFuture.then(this::mapLoaded).thenErr(this::mapLoadError);
    }

    @Override
    protected void mount() {
        super.mount();

        find(RootSection.class).setTitle(Component.text("Map Slot"));
    }

    private void mapLoaded(@NotNull MapData mapData) {
        this.map = mapData;

        if (inner == null) {
            // Remove loading button
            unmountChild(4, 2, loadingButton);
        } else {
            unmountChild(0, 0, inner);
        }

        inner = add(0, 0, new ActionView());
    }

    private void mapLoadError(@NotNull Error err) {
//         Remove loading button
//        unmountChild(4, 2, loadingButton);

        // Add error button in center todo translation
//        add(4, 2, new ButtonSection(1, 1, ItemStack.builder(Material.BARRIER)
//                .displayName(Component.text(err.message()))
//                .build(), () -> {
//        }));
        throw new RuntimeException(err.message());
    }

    // Update logic

    private void updateName(@NotNull String name) {
        map.setName(name);
        mapLoaded(map);

        saveMap(); // Save in background
    }

    private void saveMap() {
        var mapStorage = getContext(HubServer.class).mapStorage();
        mapStorage.updateMap(map).thenErr(this::mapLoadError); //todo should refetch the map incase we have a desynced state.
    }

    // Buttons

    private class ActionView extends ParentSection {
        public ActionView() {
            super(9, 6);
            add(0, 5, new BackOrCloseButton());
            add(4, 0, new MapSlotButton(rawSlot, FutureResult.of(map)));

            add(3, 2, new TranslatedButtonSection(
                    "gui.slot_view.edit_map", List.of(),
                    Material.DIAMOND_PICKAXE, this::handleEditMap
            ));
            add(5, 2, new TranslatedButtonSection(
                    "gui.slot_view.verify_map", List.of(),
                    Material.DIAMOND_BOOTS, this::handleVerifyMap
            ));
            add(2, 3, new TranslatedButtonSection(
                    "gui.slot_view.set_display_item", List.of(),
                    Material.ITEM_FRAME, this::handleSetDisplayItem
            ));
            add(4, 3, new TranslatedButtonSection(
                    "gui.slot_view.set_name", List.of(Component.text(map.getName())),
                    Material.ANVIL, this::handleEditName
            ));
            add(6, 3, new TranslatedButtonSection(
                    "gui.map_slot.set_tags", List.of(),
                    Material.NAME_TAG, this::handleEditTags
            ));
            add(3, 5, new TranslatedButtonSection(
                    "gui.map_slot.delete_map", List.of(),
                    Material.CAULDRON, this::handleDeleteMap
            ));
            add(5, 5, new TranslatedButtonSection(
                    "gui.map_slot.copy", List.of(),
                    Material.STRING, this::handleCopyMap
            ));
        }

        private boolean handleEditMap(@NotNull Player player, int slot, @NotNull ClickType clickType) {
            //todo
            return ClickHandler.DENY;
        }

        private void handleVerifyMap() {
            //todo
        }

        private void handleSetDisplayItem() {
            //todo
        }

        private void handleEditName() {
            var router = find(RouterSection.class);
            router.push(new GenericNameInput(map.getName(), MapSlotView.this::updateName));
        }

        private void handleEditTags() {
            //todo
        }

        private void handleDeleteMap() {
            var handler = getContext(Handler.class);
            handler.deleteMap(map.getId()).then(unused -> {
                var router = find(RouterSection.class);
                router.pushNew(new CreateMapsView());
            }).thenErr(err -> {
                logger.error("failed to delete mep {}: {}", map.getId(), err.message());
                //todo show error in gui
            });
        }

        private void handleCopyMap() {

        }

    }

}
