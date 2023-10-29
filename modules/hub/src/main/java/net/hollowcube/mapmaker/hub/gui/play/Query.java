package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

public class Query extends View {
    private @ContextObject String query;
    private @Outlet("map_query") Label searchButton;

    public Query(@NotNull Context context) {
        super(context);

        searchButton.setState(Element.State.LOADING);
        async(this::updateLore);
    }

    @Action("map_query")
    private @NonBlocking void beginSearchQuery() {
        pushView(QueryMaps::new);
    }

    /** Builds and updates the arg list of the query button. */
    private @Blocking void updateLore() {
        if (query != null && !query.isBlank()) {
            searchButton.setArgs(Component.text(query, TextColor.color(0x30FBFF))); // Light Blue
        } else {
            searchButton.setArgs(Component.text("None", TextColor.color(0xFF2D2D))); // Red
        }
        searchButton.setState(Element.State.ACTIVE);
    }
}
