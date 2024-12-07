package net.hollowcube.mapmaker.map.feature.play.item;

import net.hollowcube.mapmaker.map.feature.play.handlers.SpectateHandler;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ExitSpectatorModeItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/exit_spectator"));
    public static final String ID = "mapmaker:exit_spectator";
    public static final ExitSpectatorModeItem INSTANCE = new ExitSpectatorModeItem();

    private ExitSpectatorModeItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        SpectateHandler.setSpectating(click.player(), false);
    }
}
