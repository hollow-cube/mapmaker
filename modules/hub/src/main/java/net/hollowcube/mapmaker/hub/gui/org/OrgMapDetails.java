package net.hollowcube.mapmaker.hub.gui.org;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.hub.gui.edit.AbstractMapEditor;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OrgMapDetails extends AbstractMapEditor {
    public static final String SIG_UPDATE = "org_map_details_update";
    public static final String SIG_RESET = "org_map_details_reset";

    private @ContextObject ServerBridge bridge;
    private @ContextObject MapService mapService;
    private @ContextObject Player player;

    private @Outlet("root") Switch rootSwitch;


    public OrgMapDetails(@NotNull Context context) {
        super(context);
    }

    @Override
    protected void updateElementsFromMap() {
        rootSwitch.setOption(map == null ? 0 : 1);
        if (map == null) return;

        super.updateElementsFromMap();

        // Triggers the entry with the same map to update itself to reflect the new changes
        performSignal(SIG_UPDATE, map.id());
    }

    public void setMap(@Nullable MapData map) {
        this.map = map;
        updateElementsFromMap();
    }

    @Action(value = "edit_map", async = true)
    public void handleEditMap(@NotNull Player player) {
        try {
            player.closeInventory();
            bridge.joinMap(player, map.id(), ServerBridge.JoinMapState.EDITING);
        } catch (Exception e) {
            player.sendMessage(Component.translatable("edit.map.failure"));
            MinecraftServer.getExceptionManager().handleException(e);
            player.closeInventory();
        }
    }

    @Action(value = "publish_map", async = true)
    public void handlePublishMap(@NotNull Player player) {
        player.closeInventory();
        player.sendMessage("todo: publish");
    }

    @Action(value = "delete_map", async = true)
    public void handleDeleteMap(@NotNull Player player) {
        try {
            var mapPlayerData = MapPlayerData.fromPlayer(player);
            mapService.deleteMap(mapPlayerData.id(), map.id(), null);
            performSignal(SIG_RESET);
        } catch (Exception e) {
            player.closeInventory();
            player.sendMessage(Component.translatable("command.map.delete.failure"));
        }
    }
}
