package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.hub.Handler;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

public class EditMap extends View {
    private static final System.Logger logger = System.getLogger(EditMap.class.getSimpleName());

    private @ContextObject("handler") Handler mapHandler;

    private MapData map;

    public EditMap(@NotNull Context context) {
        super(context);

        setState(State.LOADING);
    }

    public void showMap(@NotNull MapData map) {
        this.map = map;

        setState(State.ACTIVE);
    }

    @Action(value = "edit_in_world", async = true)
    private @Blocking void editMap(@NotNull Player player) {
        try {
            mapHandler.editMap(player, map.getId());
            player.closeInventory();
        } catch (Exception e) {
            //todo record this exception in sentry or something
            logger.log(System.Logger.Level.ERROR, "Failed to edit map", e);
        }
    }

    @Action(value = "publish", async = true)
    private @Blocking void publishMap(@NotNull Player player) {
        var playerData = PlayerData.fromPlayer(player);
        try {
            mapHandler.publishMap(playerData.getId(), map.getId());
            player.closeInventory();
        } catch (Exception e) {
            //todo record this exception in sentry or something
            logger.log(System.Logger.Level.ERROR, "Failed to publish map", e);
        }
    }

}
