package net.hollowcube.mapmaker.gui.play;

import net.hollowcube.canvas.ClickType;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class Query extends View {
    private @ContextObject String query;
    private @Outlet("map_query") Label searchButton;

    private final Context context;

    public Query(@NotNull Context context) {
        super(context);
        this.context = context;
        searchButton.setState(State.LOADING);
        async(this::updateLore);
    }

    @Action("map_query")
    private @NonBlocking void beginSearchQuery(@NotNull Player player, int slot, @NotNull ClickType clickType) {
        switch (clickType) {
            case SHIFT_LEFT_CLICK -> pushView(c -> new PlayMapsView(context.with(Map.of("query", !query.isBlank() ? "" : query))));
            case LEFT_CLICK -> pushView(QueryMapsView::new);
        }
    }
    /**
     * Updates the query button with your current query.
     */
    private @Blocking void updateLore() {
        if (query != null && !query.isBlank()) {
            searchButton.setArgs(Component.text(query, TextColor.color(0x30FBFF))); // Light Blue
        } else {
            searchButton.setArgs(Component.text("None", TextColor.color(0xFF2D2D))); // Red
        }
        searchButton.setState(State.ACTIVE);
    }
}
