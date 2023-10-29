package net.hollowcube.mapmaker.hub.gui.play;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class QueryMaps extends View {

    private String query = null;
    private final Context context;

    public QueryMaps(@NotNull Context context) {
        super(context);
        this.context = context;
    }

    @Action("confirmation")
    private void confirm_query() {
        if (this.query == null || this.query.isBlank()) {
            popView();
        } else {
            pushView(c -> new PlayMaps(context.with(Map.of("query", query))));
        }
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(@NotNull String input) {
        this.query = input;
    }
}
