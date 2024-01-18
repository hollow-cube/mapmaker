package net.hollowcube.mapmaker.hub.gui.org;

import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.MapSearchRequest;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class OrgMapsView extends View {

    private @ContextObject MapService mapService;
    private @ContextObject Player player;

    private final String orgId;

    public OrgMapsView(@NotNull Context context, @NotNull String orgId) {
        super(context);

        this.orgId = orgId;
    }

    @Action(value = "paging", async = true)
    private void fetchPage(@NotNull Pagination.PageRequest<OrgMapEntry> request) {
        try {
            var playerId = PlayerDataV2.fromPlayer(player).id();
            var queryResult = mapService.searchMaps(MapSearchRequest.builder(playerId)
                    .page(request.page(), request.pageSize())
                    .owner(orgId).isPublished(false)
                    .build());

            var maps = new ArrayList<OrgMapEntry>();

            for (var map : queryResult.results()) {
                maps.add(new OrgMapEntry(request.context(), orgId, map));
            }

            if (maps.size() < request.pageSize()) {
                // We are on the last page
                maps.add(new OrgMapEntry(request.context(), orgId, null));
            }

            request.respond(maps, queryResult.nextPage());

//            maxPages = request.page() + 1;
//            currentPage = request.page() + 1;
//            updatePageText();
        } catch (Exception e) {
            player.closeInventory();
            player.sendMessage(Component.translatable("generic.unknown_error"));
            MinecraftServer.getExceptionManager().handleException(e);
        }
    }

}
