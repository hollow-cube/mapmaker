package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.ItemHandler;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import static net.hollowcube.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

public class ResetToSpectatorCheckpointItem extends ItemHandler {

    public static final String ID = "mapmaker:reset_to_spectator_checkpoint";
    public static final ResetToSpectatorCheckpointItem INSTANCE = new ResetToSpectatorCheckpointItem();

    private ResetToSpectatorCheckpointItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.RED_DYE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var checkpoint = player.getTag(SPECTATOR_CHECKPOINT);
        if (checkpoint != null) // TODO this shouldn't be possible but if it is what do?
            player.teleport(checkpoint);
    }
}
