package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ResetSaveStateItem extends ItemHandler {

    public static final String ID = "mapmaker:reset_savestate";
    public static final ResetSaveStateItem INSTANCE = new ResetSaveStateItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/reset"));

    private ResetSaveStateItem() {
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

        //todo need to handle spectators separately
        var world = MapWorld.forPlayerOptional(player);
        if (world instanceof PlayingMapWorld || world instanceof TestingMapWorld) {
            EventDispatcher.call(new MapPlayerResetEvent(player, world, false));
        }

//
//        var world = (InternalMapWorld) MapWorld.forPlayerOptional(player);
//        if (world == null) return;
//        //todo this cast is bad, should redo this whole thing
//
//        // Delete the save state
//        var saveState = SaveState.optionalFromPlayer(player);
//        if (saveState != null) {
//            if (world instanceof PlayingMapWorld playingWorld) playingWorld.removePlayer(player, false);
//            else world.removePlayer(player);
//
//            Thread.startVirtualThread(() -> {
//                try {
//                    // Delete the saveState
//                    world.server().mapService().deleteSaveState(world.map().id(), saveState.playerId(), saveState.id());
//                } catch (Exception ignored) {
//                    // doesn't really matter.
//                }
//
//                world.acceptPlayer(player, true);
//            });
//        } else {
//            // The player has no save state because they are spectating, so just re-add them to the server
//            if (world instanceof PlayingMapWorld playingWorld) playingWorld.removePlayer(player, false);
//            else world.removePlayer(player);
//            world.acceptPlayer(player, true);
//        }
    }

}
