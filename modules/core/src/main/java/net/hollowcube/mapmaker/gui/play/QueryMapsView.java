package net.hollowcube.mapmaker.gui.play;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Text;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class QueryMapsView extends View {

    private String query = null;
    private final Context context;

    @Outlet("title") Text titleText;

    public QueryMapsView(@NotNull Context context) {
        super(context);
        this.context = context;
        titleText.setText("Search Maps");
    }

    @Action("input") public void handleBackButton() { popView(); }

    @Action("output")
    private void confirm_query() {
        if (this.query == null || this.query.isBlank()) {
            popView();
        } else {
            pushView(c -> new PlayMapsView(context.with(Map.of("query", query))));
        }
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(@NotNull String input) {
        this.query = input;
    }
}
