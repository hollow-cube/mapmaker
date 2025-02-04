package net.hollowcube.common.components;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TranslatableBuilder {

    private final String key;
    private final List<Component> args = new ArrayList<>();

    public TranslatableBuilder(@NotNull String key) {
        this.key = key;
    }

    @Contract("_ -> this")
    public @NotNull TranslatableBuilder with(@NotNull String text) {
        return with(Component.text(text));
    }

    @Contract("_ -> this")
    public @NotNull TranslatableBuilder with(@NotNull Number number) {
        return switch (number) {
            case Integer i -> with(Component.text(i));
            case Double d -> with(Component.text(d));
            case Float f -> with(Component.text(f));
            case Long l -> with(Component.text(l));
            default -> with(number.toString());
        };
    }

    @Contract("_ -> this")
    public @NotNull TranslatableBuilder with(@NotNull Component component) {
        this.args.add(component);
        return this;
    }

    public @NotNull Component build() {
        return LanguageProviderV2.translateMultiMerged(this.key, this.args);
    }
}
