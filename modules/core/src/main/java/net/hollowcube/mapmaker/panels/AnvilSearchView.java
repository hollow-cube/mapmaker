package net.hollowcube.mapmaker.panels;

import net.kyori.adventure.text.Component;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public class AnvilSearchView<T> extends AbstractAnvilView {
    private final BiFunction<String, Integer, List<T>> searchFunction;
    private final String defaultSearchTerm;
    private final Function<T, Button> buttonFactory;
    private final Consumer<T> onSubmit;

    private final ItemContainer itemContainer;

    private String text = "";
    private Task task = null;

    public AnvilSearchView(
        @NotNull String icon, @NotNull String title,
        @NotNull BiFunction<String, Integer, List<T>> searchFunction,
        @NotNull Function<T, Button> buttonFactory,
        @NotNull Consumer<T> onSubmit
    ) {
        // "s" is kinda of a weird default, but it was set for block inputs previously and works fine.
        this(icon, title, searchFunction, "s", buttonFactory, onSubmit);
    }

    public AnvilSearchView(
        @NotNull String icon, @NotNull String title,
        @NotNull BiFunction<String, Integer, List<T>> searchFunction,
        @NotNull String defaultSearchTerm,
        @NotNull Function<T, Button> buttonFactory,
        @NotNull Consumer<T> onSubmit
    ) {
        super("generic2/anvil/search_container", icon, title, "", false);

        this.searchFunction = searchFunction;
        this.defaultSearchTerm = defaultSearchTerm;
        this.buttonFactory = buttonFactory;
        this.onSubmit = onSubmit;

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
            this.itemContainer.updateContents();
        }).delay(500, TimeUnit.MILLISECOND).schedule();
    }

    private class ItemContainer extends Panel {
        public ItemContainer() {
            super(9, 3);

            updateContents();
        }

        private void updateContents() {
            clear();
            var results = searchFunction.apply(text.isEmpty() ? defaultSearchTerm : text, 9 * 3);
            for (int i = 0; i < Math.min(results.size(), 9 * 3); i++) {
                int x = i % 9, y = i / 9;
                final T value = results.get(i);
                add(x, y, buttonFactory.apply(value)
                    .onLeftClick(() -> {
                        onSubmit.accept(value);
                        host.popView();
                    }));
            }
        }
    }

}
