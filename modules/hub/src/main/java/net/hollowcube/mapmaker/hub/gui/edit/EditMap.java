package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.hub.Handler;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EditMap extends View {

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

    @Action("edit_in_world")
    private void editMap(@NotNull Player player) {
        mapHandler.editMap(player, map.getId())
                .then(unused -> player.closeInventory())
                .thenErr(err -> {
                    throw new RuntimeException(err.message());
                });
    }

    @Action("publish")
    private void publishMap(@NotNull Player player) {
        var playerData = PlayerData.fromPlayer(player);
        mapHandler.publishMap(playerData.getId(), map.getId())
                .then(unused -> player.closeInventory())
                .thenErr(err -> {
                    throw new RuntimeException(err.message());
                });
    }

}
