package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class EditMap extends View {

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
        var mapHandler = HubServer.StaticAbuse.handler;

        mapHandler.editMap(player, map.getId())
                .then(unused -> player.closeInventory());
    }

    @Action("publish")
    private void publishMap() {
        System.out.println("Publishing map... maybe :D");
    }

}
