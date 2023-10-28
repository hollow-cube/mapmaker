package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.hollowcube.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

public class ReturnToSpectatorCheckpointItem extends ItemHandler {

    public static final String ID = "mapmaker:return_to_spectator_checkpoint";
    public static final ReturnToSpectatorCheckpointItem INSTANCE = new ReturnToSpectatorCheckpointItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/return_to_spectator_checkpoint"));

    private ReturnToSpectatorCheckpointItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.DIAMOND;
    }

    @Override
    public int customModelData() {
        return SPRITE.cmd();
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var checkpoint = player.getTag(SPECTATOR_CHECKPOINT);
        if (checkpoint != null) // TODO this shouldn't be possible but if it is what do?
            player.teleport(checkpoint);
    }
}
