package net.hollowcube.mapmaker.hub.gui.map;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RootSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.mapmaker.hub.gui.common.BackOrCloseButton;
import net.hollowcube.mapmaker.hub.gui.common.TranslatedButtonSection;
import net.hollowcube.mapmaker.hub.gui.map.component.MapSlotButton;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.result.Error;
import net.hollowcube.mapmaker.result.FutureResult;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class MapSlotView extends ParentSection {
    private final ButtonSection loadingButton;

    public MapSlotView(int slot, @Nullable FutureResult<MapData> mapFuture) {
        super(9, 6);
        add(0, 5, new BackOrCloseButton());

        //todo for gui lib
        // better define removing something. Should be able to remove by any single one of its indices,
        // and should handle a component overlapping by throwing an exception.

        // Add loading button
        loadingButton = add(4, 2, new ButtonSection(1, 1, MapSlotButton.LOADING_ICON));

        if (mapFuture == null) {
            throw new RuntimeException("not implemented");
        }

        add(4, 0, new MapSlotButton(slot, mapFuture));
        mapFuture.then(this::mapLoaded).thenErr(this::mapLoadError);
    }

    @Override
    protected void mount() {
        super.mount();

        find(RootSection.class).setTitle(Component.text("Map Slot"));
    }

    private void mapLoaded(@NotNull MapData mapData) {
        // Remove loading button
        unmountChild(4, 2, loadingButton);

        addEditButton();
        addVerifyButton();
        addDisplayItemButton();
        addNameButton();
        addTagsButton();

        addDeleteButton();
        addCopyButton();
    }

    private void mapLoadError(@NotNull Error err) {
        // Remove loading button
        unmountChild(4, 2, loadingButton);

        // Add error button in center todo translation
        add(4, 2, new ButtonSection(1, 1, ItemStack.builder(Material.BARRIER)
                .displayName(Component.text(err.message()))
                .build(), () -> {
        }));
    }

    // Buttons

    private void addEditButton() {
        add(3, 2, new TranslatedButtonSection(
                "gui.map_slot.edit", List.of(),
                Material.DIAMOND_PICKAXE, this::handleEditMap
        ));
    }

    private boolean handleEditMap(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        //todo
        return ClickHandler.DENY;
    }

    private void addVerifyButton() {
        add(5, 2, new TranslatedButtonSection(
                "gui.map_slot.verify", List.of(),
                Material.DIAMOND_BOOTS, this::handleVerifyMap
        ));
    }

    private void handleVerifyMap() {
        //todo
    }

    private void addDisplayItemButton() {
        add(2, 3, new TranslatedButtonSection(
                "gui.map_slot.displayitem", List.of(),
                Material.ITEM_FRAME, this::handleSetDisplayItem
        ));
    }

    private void handleSetDisplayItem() {
        //todo
    }

    private void addNameButton() {
        add(4, 3, new TranslatedButtonSection(
                "gui.map_slot.name", List.of(),
                Material.ANVIL, this::handleEditName
        ));
    }

    private void handleEditName() {
        //todo
    }

    private void addTagsButton() {
        add(6, 3, new TranslatedButtonSection(
                "gui.map_slot.tags", List.of(),
                Material.NAME_TAG, this::handleEditTags
        ));
    }

    private void handleEditTags() {
        //todo
    }

    private void addDeleteButton() {
        add(3, 5, new TranslatedButtonSection(
                "gui.map_slot.delete", List.of(),
                Material.CAULDRON, this::handleDeleteMap
        ));
    }

    private void handleDeleteMap() {
        //todo
    }

    private void addCopyButton() {
        add(5, 5, new TranslatedButtonSection(
                "gui.map_slot.copy", List.of(),
                Material.STRING, this::handleCopyMap
        ));
    }

    private void handleCopyMap() {

    }

}
