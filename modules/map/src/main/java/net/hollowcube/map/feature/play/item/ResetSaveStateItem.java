package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.MapHooks;
import net.hollowcube.map.event.MapPlayerInitEvent;
import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.kyori.adventure.text.Component;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

public class ResetSaveStateItem extends ItemHandler {

    public static final String ID = "mapmaker:reset_savestate";
    public static final ResetSaveStateItem INSTANCE = new ResetSaveStateItem();

    private ResetSaveStateItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.REDSTONE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        player.sendMessage(Component.translatable("map.status.reset.confirm"));

        var world = (PlayingMapWorld) MapWorld.forPlayer(player);
        //todo this cast is bad, should redo this whole thing

        // Delete the save state
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState != null) {
            player.removeTag(MapHooks.PLAYING);
            saveState.setPlaytime(0);
            saveState.setPlayStartTime(System.currentTimeMillis());
            saveState.setCompleted(false);
            saveState.setCheckpoint(null, world.map().settings().getSpawnPoint());
            player.teleport(world.map().settings().getSpawnPoint()).join();
            //todo this will not clear effects or anything, i guess the plate fp will have to do that
            player.setTag(MapHooks.PLAYING, true);
            EventDispatcher.call(new MapPlayerInitEvent(world, player, true));
        } else {
            // The player has no save state because they are spectating, so just re-add them to the server
            world.removePlayer(player, false);
            world.acceptPlayer(player, true);
        }
    }

}
