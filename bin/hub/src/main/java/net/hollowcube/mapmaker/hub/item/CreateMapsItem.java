package net.hollowcube.mapmaker.hub.item;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.hub.gui.create.CreateMapsView;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class CreateMapsItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hammer"), "hammer");
    public static final Key ID = Key.key("mapmaker:create_maps");

    private final ApiClient api;
    private final PlayerService playerService;
    private final ServerBridge bridge;

    public CreateMapsItem(ApiClient api, PlayerService playerService, ServerBridge bridge) {
        super(ID, RIGHT_CLICK_ANY);
        this.api = api;
        this.playerService = playerService;
        this.bridge = bridge;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();

        FutureUtil.submitVirtual(() -> {
            try {
                CreateMapsView.open(player, api, playerService, bridge);
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player);
            }
        });
    }

}
