package net.hollowcube.mapmaker.hub.gui.org;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerData;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class OrgMapsView extends View {

    private @ContextObject MapService mapService;
    private @ContextObject Player player;

    private @Outlet("title") Text titleText;
    private @Outlet("paging") Pagination pagination;
    private @Outlet("details") OrgMapDetails details;

    private final String orgId;
    private MapData selectedMap = null;

    public OrgMapsView(@NotNull Context context, @NotNull String orgId) {
        super(context);
        this.orgId = orgId;

        titleText.setText("Hollow Cube (or not)");
    }

    @Action(value = "paging", async = true)
    private void fetchPage(@NotNull Pagination.PageRequest<OrgMapEntry> request) {
        try {
            var playerId = PlayerData.fromPlayer(player).id();
            var queryResult = mapService.searchOrgMaps(playerId, request.page(), request.pageSize(), orgId);

            var maps = new ArrayList<OrgMapEntry>();

            MapData firstMap = null;
            for (var map : queryResult.results()) {
                if (firstMap == null) firstMap = map;
                maps.add(new OrgMapEntry(request.context(), orgId, map));
            }

            if (maps.size() < request.pageSize()) {
                // We are on the last page
                maps.add(new OrgMapEntry(request.context(), orgId, null));
            }

            request.respond(maps, queryResult.nextPage());

            if (this.selectedMap == null && firstMap != null) {
                handleSelectMap(firstMap);
            } else {
                details.setMap(null);
            }

//            maxPages = request.page() + 1;
//            currentPage = request.page() + 1;
//            updatePageText();
        } catch (Exception e) {
            player.closeInventory();
            player.sendMessage(Component.translatable("generic.unknown_error"));
            ExceptionReporter.reportException(e, player);
        }
    }

    @Signal(OrgMapEntry.SIG_SELECT_MAP)
    public void handleSelectMap(@NotNull MapData map) {
        selectedMap = map;
        details.setMap(map);
    }

    @Signal(OrgMapEntry.SIG_MAP_ADDED)
    public void handleMapAdded() {
        pagination.reset();
    }

    @Signal(OrgMapDetails.SIG_RESET)
    public void handleMapDelete() {
        selectedMap = null;
        pagination.reset();
    }
}
