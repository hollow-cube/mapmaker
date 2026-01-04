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

    public static TranslatableBuilder of(@NotNull String key) {
        return new TranslatableBuilder(key);
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

    @Contract("_ -> this")
    public @NotNull TranslatableBuilder withAll(@NotNull List<Component> args) {
        this.args.addAll(args);
        return this;
    }

    @Contract("_ -> this")
    public @NotNull TranslatableBuilder withTranslation(@NotNull String key) {
        return with(Component.translatable(key));
    }

    public @NotNull Component build() {
        return LanguageProviderV2.translateMultiMerged(this.key, this.args);
    }

    public @NotNull Component toComponent() {
        return LanguageProviderV2.translateMultiMerged(this.key, this.args);
    }

    public @NotNull List<Component> toList() {
        return LanguageProviderV2.translateMulti(this.key, this.args);
    }
}
