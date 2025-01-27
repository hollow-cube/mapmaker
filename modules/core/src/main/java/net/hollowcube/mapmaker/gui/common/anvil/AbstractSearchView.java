package net.hollowcube.mapmaker.gui.common.anvil;

import net.hollowcube.canvas.Element;
import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.Pagination;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.annotation.Signal;
import net.hollowcube.canvas.internal.Context;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.item.ItemComponent;
import net.minestom.server.timer.Task;
import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.util.List;

/**
 * A generic base class for search views.
 * The following outlets are required:
 * - input: <button id="input"/>
 * - info: <item id="info"/>
 * - output: <item id="output"/>
 * - page: <pagination id="page"/>
 * - no_results: <sprite id="no_results"/> // must be in first slot after 'page'
 */
public abstract class AbstractSearchView<T extends View> extends View {

    private @Outlet("input") Label input;
    private @Outlet("no_results") Element noResults;

    private @Outlet("page") Pagination pagination;

    protected Duration debounce = Duration.ofMillis(500);

    private String lastInput;
    private Task debounceTask;

    protected AbstractSearchView(@NotNull Context context) {
        this(context, "");
    }

    protected AbstractSearchView(@NotNull Context context, @NotNull String input) {
        super(context);

        this.lastInput = input;
        this.input.setItemSprite(this.input.getItemDirect().with(ItemComponent.HIDE_TOOLTIP));
        this.input.setArgs(Component.text(input));
    }

    @Signal(Element.SIG_ANVIL_INPUT)
    private void handleInput(String input) {
        if (input.equals(this.lastInput)) return;

        this.lastInput = input;

        if (this.debounceTask != null) this.debounceTask.cancel();
        this.debounceTask = MinecraftServer.getSchedulerManager().buildTask(() -> {
            this.input.setArgs(Component.text(this.lastInput));
            this.pagination.reset();
        }).delay(this.debounce).schedule();
    }

    @Action("input")
    private void handleBackButton() {
        popView();
    }

    @Action("page")
    private void createPage(@NotNull Pagination.PageRequest<T> request) {
        var result = search(request.context(), request.page(), request.pageSize(), this.lastInput);
        this.noResults.setState(result.isEmpty() ? State.ACTIVE : State.HIDDEN);
        request.respond(result, false);
    }

    protected abstract List<T> search(@NotNull Context context, int page, int size, @NotNull String input);

}
