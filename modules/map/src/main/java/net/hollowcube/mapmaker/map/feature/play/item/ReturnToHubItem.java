package net.hollowcube.mapmaker.map.feature.play.item;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.feature.play.handlers.SpectateHandler;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Objects;

public class ReturnToHubItem extends ItemHandler {
    private static final Logger logger = LoggerFactory.getLogger(ReturnToHubItem.class);

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/return_to_hub"));
    public static final String ID = "mapmaker:return_to_hub";
    public static final ReturnToHubItem INSTANCE = new ReturnToHubItem();

    private ReturnToHubItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return; // Sanity

        FutureUtil.submitVirtual(() -> {
            try {
                SpectateHandler.setCheckpoint(player, null);
                world.removePlayer(player);
                world.server().bridge().joinHub(player);
            } catch (Exception e) {
                logger.error("failed to send player {} to hub: {}", player.getUuid(), e.getMessage());
                LanguageProviderV2.translateMulti("command.generic.unknown_error", List.of())
                        .forEach(player::sendMessage);
            }
        });
    }

}
