package net.hollowcube.mapmaker.hub.item;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.CoreFeatureFlags;
import net.hollowcube.mapmaker.hub.gui.create.CreateMapsView;
import net.hollowcube.mapmaker.hub.gui.edit.CreateMaps;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CreateMapsItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hammer"), "hammer");
    public static final Key ID = Key.key("mapmaker:create_maps");

    private final MapService mapService;
    private final ServerBridge bridge;
    private final Controller guiController;

    public CreateMapsItem(MapService mapService, ServerBridge bridge, @NotNull Controller guiController) {
        super(ID, RIGHT_CLICK_ANY);
        this.mapService = mapService;
        this.bridge = bridge;
        this.guiController = guiController;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        if (CoreFeatureFlags.CREATE_MAPS_V2.test(player) && !player.isSneaking()) {
            var playerId = PlayerData.fromPlayer(player).id();
//            System.out.println("slots: " + mapService.getPlayerMapSlots(playerId));
            Panel.open(player, new CreateMapsView(mapService, bridge));
            return;
        }

        guiController.show(player, CreateMaps::new);
    }

}
