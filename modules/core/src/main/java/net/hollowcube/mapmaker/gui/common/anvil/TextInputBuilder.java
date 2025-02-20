package net.hollowcube.mapmaker.gui.common.anvil;

import net.hollowcube.canvas.View;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.text.Component;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

public class TextInputBuilder<T, V extends View> {

    private final TextInputFactory<T, V> factory;

    Component title = Component.empty();
    BadSprite icon = BadSprite.require("anvil/speech_bubble");
    String signal = null;
    BiConsumer<V, T> callback = null;

    TextInputBuilder(TextInputFactory<T, V> factory) {
        this.factory = factory;
    }

    public TextInputBuilder<T, V> title(String title) {
        return title(Component.text(title));
    }

    public TextInputBuilder<T, V> title(Component title) {
        this.title = title;
        return this;
    }

    public TextInputBuilder<T, V> icon(String icon) {
        this.icon = BadSprite.require(icon);
        return this;
    }

    public TextInputBuilder<T, V> signal(String signal) {
        Check.stateCondition(this.callback != null, "Callback and signal cannot be set at the same time");
        this.signal = signal;
        return this;
    }

    public TextInputBuilder<T, V> callback(Consumer<T> callback) {
        return callback((view, content) -> {
            callback.accept(content);
            view.popView();
        });
    }

    public TextInputBuilder<T, V> callback(BiConsumer<V, T> callback) {
        Check.stateCondition(this.signal != null, "Callback and signal cannot be set at the same time");
        this.callback = callback;
        return this;
    }

    public V build(@NotNull Context context) {
        return build(context, null);
    }

    public V build(@NotNull Context context, @Nullable T input) {
        Check.stateCondition(this.signal == null && this.callback == null, "Either signal or callback must be set");
        return this.factory.create(context, this, input);
    }

    @FunctionalInterface
    public interface TextInputFactory<T, V extends View> {
        V create(@NotNull Context context, @NotNull TextInputBuilder<T, V> settings, @Nullable T input);
    }
}
