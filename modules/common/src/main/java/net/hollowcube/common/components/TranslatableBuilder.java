package net.hollowcube.common.components;

import net.hollowcube.common.lang.LanguageProviderV2;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.Contract;

import java.util.ArrayList;
import java.util.List;

public class TranslatableBuilder {

    private final String key;
    private final List<Component> args = new ArrayList<>();

    public TranslatableBuilder(String key) {
        this.key = key;
    }

    public static TranslatableBuilder of(String key) {
        return new TranslatableBuilder(key);
    }

    @Contract("_ -> this")
    public TranslatableBuilder with(String text) {
        return with(Component.text(text));
    }

    @Contract("_ -> this")
    public TranslatableBuilder with(Number number) {
        return switch (number) {
            case Integer i -> with(Component.text(i));
            case Double d -> with(Component.text(d));
            case Float f -> with(Component.text(f));
            case Long l -> with(Component.text(l));
            default -> with(number.toString());
        };
    }

    @Contract("_ -> this")
    public TranslatableBuilder with(Component component) {
        this.args.add(component);
        return this;
    }

    @Contract("_ -> this")
    public TranslatableBuilder withAll(List<Component> args) {
        this.args.addAll(args);
        return this;
    }

    @Contract("_ -> this")
    public TranslatableBuilder withTranslation(String key) {
        return with(Component.translatable(key));
    }

    public Component build() {
        return LanguageProviderV2.translateMultiMerged(this.key, this.args);
    }

    public Component toComponent() {
        return LanguageProviderV2.translateMultiMerged(this.key, this.args);
    }

    public List<Component> toList() {
        return LanguageProviderV2.translateMulti(this.key, this.args);
    }
}
