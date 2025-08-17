package net.hollowcube.mapmaker.editor.hdb.gui;

import net.hollowcube.canvas.*;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.editor.hdb.HeadDatabase;
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

    private @Outlet("title") Text titleText;
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

        titleText.setText("Search Heads");
        inputField.setArgs(Component.text(input));
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    public void handleAnvilInput(@NotNull String input) {
        if (this.input.equals(input)) return;

        if (task != null) task.cancel();
        task = scheduler.buildTask(() -> {
            inputField.setArgs(Component.text(input));
            pagination.reset();
        }).delay(1, TimeUnit.SECOND).schedule();

        this.input = input;
    }

    @Action("input")
    public void handleBackButton(@NotNull Player player) {
        player.closeInventory();
    }

    @Action(value = "page", async = true)
    private void createPage(@NotNull Pagination.PageRequest<HeadIconView> request) {
        List<HeadIconView> result = new ArrayList<>();
        if (input.isEmpty()) {
            // Add some random items
            for (var head : hdb.getRandom(request.pageSize())) {
                result.add(new HeadIconView(request.context(), head));
            }
        } else {
            result = new ArrayList<>();
            for (var suggestion : hdb.getSuggestions(input, request.pageSize())) {
                result.add(new HeadIconView(request.context(), suggestion));
            }

            // Show a "no results" button if there are no results
            if (result.isEmpty()) {
                result.add(new HeadIconView(request.context()));
            }
        }
        request.respond(result, result.size() <= request.pageSize());
    }
}
