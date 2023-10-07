package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.InternalMapWorld;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class ExitSpectatorModeItem extends ItemHandler {

    public static final String ID = "mapmaker:exit_spectator";
    public static final ExitSpectatorModeItem INSTANCE = new ExitSpectatorModeItem();

    private ExitSpectatorModeItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.CLAY_BALL;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = (InternalMapWorld) MapWorld.forPlayer(player);
        //todo should not depend on implementation details of InternalMapWorld

        world.removePlayer(player);
        if (world instanceof PlayingMapWorld playingWorld) {
            playingWorld.removePlayer(player, false);
            playingWorld.acceptPlayer(player, true);
        }
    }

}
