package net.hollowcube.mapmaker.hub.gui.map;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RootSection;
import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.mapmaker.hub.handler.MapHandler;
import net.hollowcube.mapmaker.result.FutureResult;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class CreateMapView extends ParentSection {
    private static final Logger logger = LoggerFactory.getLogger(CreateMapView.class);

    public static final ItemStack TEMP_BUTTON = ItemStack.of(Material.GLASS_PANE)
            .withDisplayName(Component.text("Click to create"))
            .withLore(List.of(
                    Component.text("Eventually there will be options/presets here"),
                    Component.text("but for now it is just this button")
            ));

    public static final ItemStack LOADING_ICON = ItemStack.of(Material.GLASS_PANE)
            .withDisplayName(Component.text("Loading..."));

    public static final ItemStack ERROR_ICON = ItemStack.of(Material.GLASS_PANE)
            .withDisplayName(Component.text("Failed to create map"));

    private final ButtonSection createButton = new ButtonSection(1, 1, TEMP_BUTTON, this::handleCreateButton);

    private final int slot;

    public CreateMapView(int slot) {
        super(9, 3);
        this.slot = slot;

        add(0, 0, createButton);
    }

    @Override
    protected void mount() {
        super.mount();

        var root = find(RootSection.class);
        root.setTitle(Component.text("Create Map"));
    }

    private boolean handleCreateButton(@NotNull Player player, int unused, @NotNull ClickType clickType) {
        // Swap to the loading icon
        createButton.setItem(LOADING_ICON);

        // Dispatch request to create the map with a short random name for now
        var mapHandler = getContext(MapHandler.class);
        var tempName = Integer.toString(ThreadLocalRandom.current().nextInt(1000000), 36);
        mapHandler.createMap(player, tempName, slot - 1)
                .then(map -> {
                    // Map was created, open the map view
                    //todo this is a case where the section is transient (eg should not be recorded by the history)
                    // pressing a back button from the MapSlotView should take you to the map list, not this one.
                    var router = find(RouterSection.class);
                    router.push(new MapSlotView(slot, FutureResult.of(map)));
                })
                .thenErr(e -> {
                    createButton.setItem(ERROR_ICON);
                    logger.error("failed to create map (slot={}, player={}): {}", slot, player.getUuid(), e.message());
                });
        return ClickHandler.DENY;
    }

}
