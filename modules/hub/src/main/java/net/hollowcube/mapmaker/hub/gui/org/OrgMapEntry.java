package net.hollowcube.mapmaker.hub.gui.org;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.bridge.ServerBridge;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.PersonalizedMapData;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OrgMapEntry extends View {

    private @ContextObject MapService mapService;
    private @ContextObject ServerBridge bridge;

    private @Outlet("switch") Switch buttonSwitch;

    private final String orgId;

    /**
     * Used for both a map button and the add map button. if given a null map its the add button
     */
    public OrgMapEntry(@NotNull Context context, @NotNull String orgId, @Nullable PersonalizedMapData map) {
        super(context);
        this.orgId = orgId;

        if (map != null) {
            // This is a map button, setup accordingly
        } else {
            // This is the add map button
            buttonSwitch.setOption(1);
        }
    }

    @Action(value = "add_map", async = true)
    public void handleAddMap(@NotNull Player player) {
        try {
            var playerId = PlayerDataV2.fromPlayer(player).id();
            var map = mapService.createOrgMap(playerId, orgId);
            System.out.println("created map " + map);
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
            player.sendMessage("todo something went wrong oopsie woopsie");
        }
    }

}
