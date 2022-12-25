package net.hollowcube.mapmaker.hub.gui.map;

import net.hollowcube.canvas.ClickHandler;
import net.hollowcube.canvas.ParentSection;
import net.hollowcube.canvas.RootSection;
import net.hollowcube.canvas.RouterSection;
import net.hollowcube.canvas.std.ButtonSection;
import net.hollowcube.common.result.FutureResult;
import net.hollowcube.mapmaker.hub.Handler;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.inventory.click.ClickType;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

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
    private MapData protoMap;

    public CreateMapView(@Range(from = 0, to = PlayerData.MAX_MAP_SLOTS - 1) int slot) {
        super(9, 3);
        this.slot = slot;

        add(0, 0, createButton);
    }

    @Override
    protected void mount() {
        super.mount();
        protoMap = new MapData();

        var root = find(RootSection.class);
        root.setTitle(Component.text("Create Map"));
    }

    private boolean handleCreateButton(@NotNull Player player, int unused, @NotNull ClickType clickType) {
        // Swap to the loading icon
        createButton.setItem(LOADING_ICON);

        var playerData = PlayerData.fromPlayer(player);
        protoMap.setOwner(playerData.getId());

        // Dispatch request to create the map with a short random name for now
        var mapHandler = getContext(Handler.class);
        mapHandler.createMapForPlayerInSlot(playerData, protoMap, slot)
                .then(map -> {
                    // Map was created, open the map view
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
