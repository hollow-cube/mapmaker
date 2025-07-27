package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.ProtocolVersions;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.requests.MapCreateRequest;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateContestMap extends View {
    private @ContextObject Player player;
    private @ContextObject MapService mapService;

    private @Outlet("submit") Label submitButton;

    public CreateContestMap(@NotNull Context context) {
        super(context);
    }

    @Override
    public void mount() {
        super.mount();

        submitButton.setState(State.ACTIVE);
    }

    @Action(value = "submit", async = true)
    private void handleSubmit(@NotNull Player player) {
        submitButton.setState(State.LOADING);

        var playerData = MapPlayerData.fromPlayer(player);

        // Dispatch request to create the map
        try {
            int protocolVersion = ProtocolVersions.getProtocolVersion(player);
            var createdMap = mapService.createMap(MapCreateRequest.forContest(playerData.id(), protocolVersion));
            performSignal(CreateMap.SIG_MAP_CREATED, -1, createdMap);
            submitButton.setState(State.ACTIVE);
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.sendMessage(Component.translatable("generic.unknown_error"));
            player.closeInventory();
        }
    }

}
