package net.hollowcube.mapmaker.hub.item;

import net.hollowcube.canvas.internal.Controller;
import net.hollowcube.mapmaker.gui.play.PlayMapsView;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Objects;

public class PlayMapsItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("tablet"), "tablet");
    public static final String ID = "mapmaker:play_maps";

    private final Controller guiController;

    public PlayMapsItem(@NotNull Controller guiController) {
        super(ID, RIGHT_CLICK_ANY);
        this.guiController = guiController;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        guiController.show(player, context -> new PlayMapsView(context.with(Map.of("query", ""))));
    }

}
