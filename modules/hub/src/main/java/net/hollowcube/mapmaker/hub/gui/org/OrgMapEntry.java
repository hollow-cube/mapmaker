package net.hollowcube.mapmaker.hub.gui.org;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.requests.MapCreateRequest;
import net.hollowcube.mapmaker.map.MapSize;
import net.hollowcube.mapmaker.map.runtime.ServerBridge;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.minestom.server.entity.Player;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class OrgMapEntry extends View {

    public static final String SIG_MAP_ADDED = "org_map_added";
    public static final String SIG_SELECT_MAP = "org_map_select";

    private @ContextObject MapService mapService;
    private @ContextObject ServerBridge bridge;

    private @Outlet("switch") Switch buttonSwitch;
    private @Outlet("select_map") Label icon;

    private final String orgId;
    private final MapData map;

    /**
     * Used for both a map button and the add map button. if given a null map its the add button
     */
    public OrgMapEntry(@NotNull Context context, @NotNull String orgId, @Nullable MapData map) {
        super(context);
        this.orgId = orgId;
        this.map = map;

        if (map != null) {
            // This is a map button, setup accordingly
            updateFromMap();
        } else {
            // This is the add map button
            buttonSwitch.setOption(1);
        }
    }

    public void updateFromMap() {
        if (map == null) return;

        var iconMaterial = map.settings().getIcon();
        icon.setItemSprite(ItemStack.of(iconMaterial == null ? Material.PAPER : iconMaterial));

        icon.setArgs(map.settings().getNameComponent());
    }

    @Action("select_map")
    public void handleSelectMap(@NotNull Player player) {
        performSignal(SIG_SELECT_MAP, map);
    }

    @Action(value = "add_map", async = true)
    public void handleAddMap(@NotNull Player player) {
        try {
            var playerId = PlayerDataV2.fromPlayer(player).id();
            mapService.createMap(MapCreateRequest.forOrg(playerId, orgId, MapSize.LARGE));
            performSignal(SIG_MAP_ADDED);
        } catch (Exception e) {
            ExceptionReporter.reportException(e, player);
            player.sendMessage("todo something went wrong oopsie woopsie");
        }
    }

    @Signal(OrgMapDetails.SIG_UPDATE)
    public void handleUpdateMap(@NotNull String mapId) {
        if (map == null || !map.id().equals(mapId)) return;

        updateFromMap();
    }

}
