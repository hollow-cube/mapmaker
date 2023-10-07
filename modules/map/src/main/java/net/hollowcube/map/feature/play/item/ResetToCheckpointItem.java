package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.event.MapPlayerResetTriggerEvent;
import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class ResetToCheckpointItem extends ItemHandler {

    public static final String ID = "mapmaker:reset_to_checkpoint";
    public static final ResetToCheckpointItem INSTANCE = new ResetToCheckpointItem();

    private ResetToCheckpointItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.RED_DYE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = MapWorld.forPlayer(player);
        if (world instanceof PlayingMapWorld || world instanceof TestingMapWorld) {
            EventDispatcher.call(new MapPlayerResetTriggerEvent(world, player));
        }
    }

}
