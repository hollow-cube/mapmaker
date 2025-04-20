package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Argument<T> {

    public static @NotNull ArgumentLiteral Literal(@NotNull String literal) {
        return new ArgumentLiteral(literal);
    }

    public static @NotNull ArgumentWord Word(@NotNull String id) {
        return new ArgumentWord(id);
    }

    public static @NotNull ArgumentGreedyString GreedyString(@NotNull String id) {
        return new ArgumentGreedyString(id);
    }

    public static @NotNull ArgumentBool Bool(@NotNull String id) {
        return new ArgumentBool(id);
    }

    public static @NotNull ArgumentInt Int(@NotNull String id) {
        return new ArgumentInt(id);
    }

    public static @NotNull ArgumentFloat Float(@NotNull String id) {
        return new ArgumentFloat(id);
    }

    public static @NotNull ArgumentDouble Double(@NotNull String id) {
        return new ArgumentDouble(id);
    }

    public static <E extends Enum<?>> ArgumentEnum<E> Enum(@NotNull String id, Class<E> enumType) {
        return new ArgumentEnum<>(id, enumType);
    }

    public static @NotNull ArgumentAxis Axis(@NotNull String id) {
        return new ArgumentAxis(id);
    }

    public static @NotNull ArgumentEntity Entity(@NotNull String id) {
        return new ArgumentEntity(id);
    }

    public static @NotNull ArgumentRelativeVec3 RelativeVec3(@NotNull String id) {
        return new ArgumentRelativeVec3(id);
    }

    public static @NotNull ArgumentMaterial Material(@NotNull String id) {
        return new ArgumentMaterial(id);
    }

    public static @NotNull ArgumentItemStack ItemStack(@NotNull String id) {
        return new ArgumentItemStack(id);
    }

    public static @NotNull ArgumentUUID UUID(@NotNull String id) {
        return new ArgumentUUID(id);
    }


    // Impl

    private final String id;

    private String description = null;

    private Function<CommandSender, T> defaultProvider = null;

    protected Argument(@NotNull String id) {
        this.id = id;
    }

    public @NotNull String id() {
        return id;
    }


    // Properties

    public boolean isOptional() {
        return defaultProvider != null;
    }

    public @NotNull Argument<T> defaultValue(@Nullable T value) {
        return defaultValue(sender -> value);
    }

    public @NotNull Argument<T> defaultValue(@NotNull Function<CommandSender, T> provider) {
        this.defaultProvider = provider;
        return this;
    }

    public @Nullable T getDefaultValue(@NotNull CommandSender sender) {
        return defaultProvider == null ? null : defaultProvider.apply(sender);
    }

    public @Nullable String description() {
        return description;
    }

    public @NotNull Argument<T> description(@NotNull String description) {
        this.description = description;
        return this;
    }


    // Transforms

    public <R> @NotNull Argument<R> map(@NotNull ArgumentMap.ParseFunc<T, R> mapFunc) {
        return new ArgumentMap<>(id, this, mapFunc, null);
    }

    public <R> @NotNull Argument<R> map(@NotNull ArgumentMap.ParseFunc<T, R> mapFunc, @NotNull ArgumentMap.SuggestFunc suggestFunc) {
        return new ArgumentMap<>(id, this, mapFunc, suggestFunc);
    }


    // Logic

    public abstract @NotNull ParseResult<T> parse(@NotNull CommandSender sender, @NotNull StringReader reader);

    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        // No suggestions returned by default
    }


    // Result factories

    public @NotNull ParseResult<T> partial() {
        return new ParseResult.Partial<>();
    }

    public @NotNull ParseResult<T> partial(@Nullable String message) {
        return new ParseResult.Partial<>(message);
    }

    public @NotNull ParseResult<T> success(@NotNull Supplier<@UnknownNullability T> valueFunc) {
        return new ParseResult.Success<>(valueFunc);
    }

    public @NotNull ParseResult<T> success(@UnknownNullability T value) {
        return new ParseResult.Success<>(value);
    }

    public @NotNull ParseResult<T> syntaxError() {
        return new ParseResult.Failure<>(-1);
    }

    public @NotNull ParseResult<T> syntaxError(@NotNull String message) {
        return new ParseResult.Failure<>(-1, message);
    }

    public @NotNull ParseResult<T> syntaxError(int start) {
        return new ParseResult.Failure<>(start);
    }

    public @NotNull ParseResult<T> syntaxError(int start, @Nullable String message) {
        return new ParseResult.Failure<>(start, message);
    }


}
