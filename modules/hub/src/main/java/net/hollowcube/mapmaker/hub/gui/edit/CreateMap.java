package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.hub.Handler;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateMap extends View {

    public static final String SIG_MAP_CREATED = "map_created";

    private @ContextObject("handler") Handler mapHandler;

    private @Outlet("submit") Label submitButton;

    private int slot;
    private MapData protoMap;

    public CreateMap(@NotNull Context context) {
        super(context);
    }

    @Override
    public void mount() {
        super.mount();

        protoMap = new MapData();
        submitButton.setState(State.ACTIVE);
    }

    public void setSlot(int slot) {
        this.slot = slot;
        protoMap = new MapData();
        setState(State.ACTIVE);
    }

    @Action("submit")
    private void handleSubmit(@NotNull Player player) {
        submitButton.setState(State.LOADING);

        var playerData = PlayerData.fromPlayer(player);
        protoMap.setOwner(playerData.getId());

        // Dispatch request to create the map
        mapHandler.createMapForPlayerInSlot(playerData, protoMap, slot)
                .then(map -> {
                    performSignal(SIG_MAP_CREATED, slot, map);
                    submitButton.setState(State.ACTIVE);
                })
                .thenErr(e -> {
                    throw new RuntimeException(e.message()); //todo
                });
    }

}
