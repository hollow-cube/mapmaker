package net.hollowcube.mapmaker.hub.gui.search;

import net.hollowcube.canvas.ParentSection;
import net.hollowcube.common.result.Error;
import net.hollowcube.mapmaker.hub.gui.common.BackOrCloseButton;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.MapQuery;
import net.hollowcube.mapmaker.storage.MapStorage;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Shows the given players published maps.
 */
public class PublishedMapsView extends ParentSection {
    private final MapQuery query;

    public PublishedMapsView(@NotNull String playerId) {
        super(9, 6);
        query = MapQuery.builder().author(playerId).publishedOnly(true).build();

        // Header
        add(0, 0, new BackOrCloseButton());
    }

    @Override
    protected void mount() {
        super.mount();

        // Show loading ui

        // Fetch maps based on the query
        getContext(MapStorage.class).queryMaps(query, 0, 35)
                .then(this::mapsLoaded).thenErr(this::mapsLoadFailed);
    }

    private void mapsLoaded(@NotNull List<MapData> maps) {
        //todo remove loading

        //todo show maps
    }

    private void mapsLoadFailed(@NotNull Error err) {
        //todo remove loading

        //todo show error
    }

}
