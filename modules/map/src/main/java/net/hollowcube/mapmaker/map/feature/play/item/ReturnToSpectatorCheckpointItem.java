package net.hollowcube.mapmaker.map.feature.play.item;

import net.hollowcube.mapmaker.map.feature.play.handlers.SpectateHandler;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.RelativeFlags;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ReturnToSpectatorCheckpointItem extends ItemHandler {

    public static final String ID = "mapmaker:return_to_spectator_checkpoint";
    public static final ReturnToSpectatorCheckpointItem INSTANCE = new ReturnToSpectatorCheckpointItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/return_to_spectator_checkpoint"));

    private ReturnToSpectatorCheckpointItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var checkpoint = SpectateHandler.getCheckpoint(player);
        if (checkpoint == null) return; // Sanity check

        player.teleport(checkpoint, Vec.ZERO, null, RelativeFlags.NONE);
    }
}
