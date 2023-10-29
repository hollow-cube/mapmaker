package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.lang.LanguageProviderV2;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class Query extends View {
    private @Outlet("map_query") Label searchButton;

    private final String query;

    public Query(@NotNull Context context) {
        this(context, null);
    }

    public Query(@NotNull Context context, @Nullable String query) {
        super(context);
        this.query = query;

        searchButton.setState(Element.State.LOADING);
        async(this::updateLore);
    }

    @Action("map_query")
    private @NonBlocking void beginSearchQuery() {
        pushView(QueryMaps::new);
    }

    /** Builds and updates the arg list of the map icon. */
    private @Blocking void updateLore() {

        if (query != null) {
            searchButton.setArgs(LanguageProviderV2.translateMulti("gui.play_maps.search_maps.current_query.lore", List.of(Component.text(query))));
        } else {
            searchButton.setArgs(List.of(
                    LanguageProviderV2.translateMulti("gui.play_maps.search_maps.current_query.default.lore", List.of()),
                    LanguageProviderV2.translateMulti("gui.play_maps.search_maps.lore", List.of())
            );
        }
        searchButton.setState(Element.State.ACTIVE);
    }
}
