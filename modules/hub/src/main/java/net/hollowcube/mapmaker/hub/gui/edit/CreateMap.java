package net.hollowcube.mapmaker.hub.gui.edit;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class CreateMap extends View {
    private static final System.Logger logger = System.getLogger(CreateMap.class.getSimpleName());

    public static final String SIG_MAP_CREATED = "map_created";

    private @ContextObject MapService mapService;

    private @Outlet("submit") Label submitButton;

    private @Outlet("slot_id_create") Text slotIdText;

    private int slot;

    public CreateMap(@NotNull Context context) {
        super(context);
    }

    @Override
    public void mount() {
        super.mount();

        submitButton.setState(State.ACTIVE);
    }

    public void setSlot(int slot) {
        this.slot = slot;
        slotIdText.setText(String.format("Slot #%d", slot + 1));
        slotIdText.setArgs(Component.text(slot + 1));
        setState(State.ACTIVE);
    }

    @Action(value = "submit", async = true)
    private void handleSubmit(@NotNull Player player) {
        submitButton.setState(State.LOADING);

        var playerData = MapPlayerData.fromPlayer(player);

        // Dispatch request to create the map
        try {
            var resp = mapService.createMap(playerData, slot);
            switch (resp.errorCode()) {
                case null -> {
                    performSignal(SIG_MAP_CREATED, slot, resp.payload());
                    submitButton.setState(State.ACTIVE);
                }
                //todo handle known error cases
                default -> {
                    resp.logError(player);
                    player.closeInventory();
                }
            }
//            var map = mapService.createMap(playerData, slot);
//            performSignal(SIG_MAP_CREATED, slot, map);
//            submitButton.setState(State.ACTIVE);
        } catch (Exception e) {
            logger.log(System.Logger.Level.ERROR, "Failed to create map", e);
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

}
