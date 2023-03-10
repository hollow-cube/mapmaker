package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.mapmaker.hub.HubServer;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.PlayerData;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateMap extends View {

    private @Outlet("submit") Label submitButton;

    private final int slot;
    private MapData protoMap;

    public CreateMap(int slot) {
        this.slot = slot;
    }

    @Override
    public void mount() {
        super.mount();

        protoMap = new MapData();
        submitButton.setLoading(false);
    }

    @Action("submit")
    private void handleSubmit(@NotNull Player player) {
        submitButton.setLoading(true);

        var playerData = PlayerData.fromPlayer(player);
        protoMap.setOwner(playerData.getId());

        // Dispatch request to create the map
        var mapHandler = HubServer.StaticAbuse.handler;
        mapHandler.createMapForPlayerInSlot(playerData, protoMap, slot)
                .then(map -> {
                    System.out.println("CREATED MAP!!!!!");
                })
                .thenErr(e -> {
                    throw new RuntimeException(e.message()); //todo
                });
    }

}
