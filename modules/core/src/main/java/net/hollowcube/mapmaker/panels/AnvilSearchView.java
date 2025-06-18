package net.hollowcube.mapmaker.panels;

import net.kyori.adventure.text.Component;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.MathUtils;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class AnvilSearchView<T> extends AbstractAnvilView {
    private final BiFunction<String, Integer, List<T>> searchFunction;
    private final Function<T, Button> buttonFactory;
    private final Consumer<T> onSubmit;
    private final int extraRows; // Extra rows for scrolling

    private final ItemContainer itemContainer;

    private String text = "";
    private Task task = null;

    public AnvilSearchView(
            @NotNull String icon, @NotNull String title,
            @NotNull BiFunction<String, Integer, List<T>> searchFunction,
            @NotNull Function<T, Button> buttonFactory,
            @NotNull Consumer<T> onSubmit
    ) {
        this(icon, title, 3, searchFunction, buttonFactory, onSubmit);
    }

    public AnvilSearchView(
            @NotNull String icon, @NotNull String title,
            int extraRows,
            @NotNull BiFunction<String, Integer, List<T>> searchFunction,
            @NotNull Function<T, Button> buttonFactory,
            @NotNull Consumer<T> onSubmit
    ) {
        super("generic2/anvil/search_container", icon, title, "");

        this.searchFunction = searchFunction;
        this.buttonFactory = buttonFactory;
        this.onSubmit = onSubmit;
        this.extraRows = extraRows;

        this.itemContainer = add(0, 1, new ItemContainer());
    }

    @Override
    protected void onSubmit(@NotNull String text) {
        // Do not pop on submit, need to click something below
    }

    @Override
    protected void onInputChange(@NotNull String text) {
        if (text.equals(this.text)) return;
        this.text = text;

        if (task != null) task.cancel();
        task = host.player().scheduler().buildTask(() -> {
            this.inputButton.text(Component.text(this.text), List.of());
            this.itemContainer.scroll = 0; // Reset scroll on new search
            this.itemContainer.updateContents();
        }).delay(500, TimeUnit.MILLISECOND).schedule();
    }

    private class ItemContainer extends Panel {

        private int scroll = 0;

        public ItemContainer() {
            super(9, 3);

            updateContents();
        }

        private void onScroll(int direction, int rows) {
            int scroll = this.scroll;
            this.scroll = Math.max(Math.min(this.scroll + direction, rows - 3), 0);
            if (this.scroll == scroll) return; // No change, no need to update
            this.updateContents();
        }

        private void updateContents() {
            clear();
            // Empty gives no results so we can just use "s" as a default search term.
            // Its kinda gross, but its fine for the use cases we have now :)
            var results = searchFunction.apply(text.isEmpty() ? "s" : text, 9 * (3 + extraRows));
            var resultRows = (int) Math.ceil(results.size() / 9.0);
            for (int i = 0; i < Math.min(results.size(), 9 * 3); i++) {
                int index = this.scroll * 9 + i;
                int x = i % 9, y = i / 9;
                if (index >= results.size()) break;

                final T value = results.get(index);
                final Button button = buttonFactory.apply(value)
                        .onScroll(direction -> this.onScroll(direction, resultRows))
                        .onLeftClick(() -> {
                            onSubmit.accept(value);
                            host.popView();
                        });

                add(x, y, button);
            }
        }
    }

}
