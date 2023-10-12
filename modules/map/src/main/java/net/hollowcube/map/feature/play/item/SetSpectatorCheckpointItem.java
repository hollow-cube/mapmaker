package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.ItemHandler;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.item.Material;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

public class SetSpectatorCheckpointItem extends ItemHandler {

    public static Tag<Pos> SPECTATOR_CHECKPOINT = Tag.Transient("mapmaker:spectator_checkpoint");

    public static final String ID = "mapmaker:set_spectator_checkpoint";
    public static final SetSpectatorCheckpointItem INSTANCE = new SetSpectatorCheckpointItem();

    private SetSpectatorCheckpointItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.EMERALD;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        player.setTag(SPECTATOR_CHECKPOINT, player.getPosition());
        System.out.println("player spectator checkpoint is: " + player.getTag(SPECTATOR_CHECKPOINT).toString());
    }
}
