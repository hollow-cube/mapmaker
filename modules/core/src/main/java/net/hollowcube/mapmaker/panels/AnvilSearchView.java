package net.hollowcube.mapmaker.panels;

import net.hollowcube.common.util.FutureUtil;
import net.kyori.adventure.text.Component;
import net.minestom.server.timer.Task;
import net.minestom.server.utils.time.TimeUnit;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;

public class AnvilSearchView<T> extends AbstractAnvilView {
    public static <T> Builder<T> builder() {
        return new Builder<>();
    }

    public static <T> Builder<T> builder(String icon, String title) {
        return AnvilSearchView.<T>builder().icon(icon).title(title);
    }

    public static <T> AnvilSearchView<T> simple(String icon, String title, SearchFunction<T> searchFunction,
                                                Function<T, Button> buttonFactory, Consumer<T> onSubmit) {
        return AnvilSearchView.<T>builder(icon, title)
            .searchFunction(searchFunction)
            .buttonFactory(buttonFactory)
            .onSubmit(onSubmit)
            .build();
    }

    private final SearchFunction<T> searchFunction;
    private final String defaultSearchTerm;
    private final Function<T, Button> buttonFactory;
    private final Consumer<T> onSubmit;
    private final boolean async;

    private final ItemContainer itemContainer;

    private String text = "";
    private @Nullable Task task = null;

    private AnvilSearchView(String icon, String title, SearchFunction<T> searchFunction, String defaultSearchTerm,
                            Function<T, Button> buttonFactory, Consumer<T> onSubmit, boolean async) {
        super("generic2/anvil/search_container", icon, title, "", false);

        this.searchFunction = searchFunction;
        this.defaultSearchTerm = defaultSearchTerm;
        this.buttonFactory = buttonFactory;
        this.onSubmit = onSubmit;
        this.async = async;

        this.itemContainer = add(0, 1, new ItemContainer());
    }

    @Override
    protected void onSubmit(String text) {
        // Do not pop on submit, need to click something below
    }

    @Override
    protected void onInputChange(String text) {
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
            if (async) {
                FutureUtil.submitVirtual(this::doUpdateContents);
            } else {
                this.doUpdateContents();
            }
        }

        private void doUpdateContents() {
            var results = searchFunction.search(text.isEmpty() ? defaultSearchTerm : text, 9 * 3);
            for (int i = 0; i < Math.min(results.size(), 9 * 3); i++) {
                int x = i % 9, y = i / 9;
                final T value = results.get(i);

                var button = buttonFactory.apply(value);
                if (async) {
                    button.onLeftClickAsync(() -> leftClick(value));
                } else {
                    button.onLeftClick(() -> leftClick(value));
                }
                add(x, y, button);
            }
        }

        private void leftClick(T value) {
            onSubmit.accept(value);
            host.popView();
        }
    }

    @FunctionalInterface
    public interface SearchFunction<T> {

        List<T> search(String query, int limit);
    }

    // TODO: remove probably, essentially everything is required so weird use case for builder
    public static final class Builder<T> {
        private String icon;
        private String title;
        private SearchFunction<T> searchFunction;
        private Function<T, Button> buttonFactory;
        private Consumer<T> onSubmit;
        // "s" is kinda of a weird default, but it was set for block inputs previously and works fine.
        private String defaultSearchTerm = "s";
        private boolean async = false;

        public Builder<T> icon(String icon) {
            this.icon = Objects.requireNonNull(icon, "icon");
            return this;
        }

        public Builder<T> title(String title) {
            this.title = Objects.requireNonNull(title, "title");
            return this;
        }

        public Builder<T> searchFunction(SearchFunction<T> searchFunction) {
            this.searchFunction = Objects.requireNonNull(searchFunction, "searchFunction");
            return this;
        }

        public Builder<T> buttonFactory(Function<T, Button> buttonFactory) {
            this.buttonFactory = Objects.requireNonNull(buttonFactory, "buttonFactory");
            return this;
        }

        public Builder<T> onSubmit(Consumer<T> onSubmit) {
            this.onSubmit = Objects.requireNonNull(onSubmit, "onSubmit");
            return this;
        }

        public Builder<T> defaultSearchTerm(@Nullable String defaultSearchTerm) {
            this.defaultSearchTerm = Objects.requireNonNullElse(defaultSearchTerm, "s");
            return this;
        }

        public Builder<T> async() {
            this.async = true;
            return this;
        }

        public AnvilSearchView<T> build() {
            Objects.requireNonNull(this.searchFunction, "searchFunction");
            Objects.requireNonNull(this.buttonFactory, "buttonFactory");
            Objects.requireNonNull(this.onSubmit, "onSubmit");
            return new AnvilSearchView<>(this.icon, this.title, this.searchFunction, this.defaultSearchTerm,
                this.buttonFactory, this.onSubmit, this.async);
        }
    }
}
