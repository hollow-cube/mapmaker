package net.hollowcube.mapmaker.map.hdb.gui;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.map.hdb.HeadDatabase;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class HdbSearchView extends View {

    private @ContextObject HeadDatabase hdb;
    private @ContextObject Scheduler scheduler;

    private @Outlet("input") Label inputField;
    private @Outlet("page") Pagination pagination;

    private String input;
    private Task task = null;

    public HdbSearchView(@NotNull Context context) {
        this(context, "");
    }

    public HdbSearchView(@NotNull Context context, @NotNull String input) {
        super(context);
        this.input = input;

        inputField.setArgs(Component.text(input));
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(@NotNull String input) {
        if (this.input.equals(input)) return;

        if (task != null) task.cancel();
        task = scheduler.buildTask(() -> {
            inputField.setArgs(Component.text(input));
            pagination.reset();
        }).delay(500, TimeUnit.MILLISECOND).schedule();

        this.input = input;
    }

    @Action("input")
    public void handleBackButton(@NotNull Player player) {
        player.closeInventory();
    }

    @Action("page")
    private void createPage(@NotNull Pagination.PageRequest<HeadIconView> request) {
        List<HeadIconView> result;
        if (input.isEmpty()) {
            // Add some random items
            result = hdb.random()
                    .limit(request.pageSize())
                    .map(head -> new HeadIconView(request.context(), head))
                    .toList();
        } else {
            result = new ArrayList<>();
            for (var suggestion : hdb.suggest(input, request.pageSize())) {
                result.add(new HeadIconView(request.context(), suggestion));
            }

            // Show a "no results" button if there are no results
            if (result.isEmpty()) {
                result.add(new HeadIconView(request.context()));
            }
        }
        request.respond(result, false);
    }
}
