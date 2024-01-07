package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.UnknownNullability;

public abstract class Argument2<T> {

    public static @NotNull Argument2Literal Literal(@NotNull String literal) {
        return new Argument2Literal(literal);
    }

    public static @NotNull Argument2Word Word(@NotNull String id) {
        return new Argument2Word(id);
    }

    public static @NotNull Argument2Int Int(@NotNull String id) {
        return new Argument2Int(id);
    }


    // Impl

    private final String id;

//    private Function<CommandSender, T> defaultProvider = null;

    protected Argument2(@NotNull String id) {
        this.id = id;
    }

    public @NotNull String id() {
        return id;
    }


    // Properties

//    public boolean isOptional() {
//        return defaultProvider != null;
//    }
//
//    public @NotNull Argument2<T> defaultValue(@Nullable T value) {
//        return defaultValue(sender -> value);
//    }
//
//    public @NotNull Argument2<T> defaultValue(@NotNull Function<CommandSender, T> provider) {
//        this.defaultProvider = provider;
//        return this;
//    }
//
//    public @Nullable T getDefaultValue(@NotNull CommandSender sender) {
//        return defaultProvider == null ? null : defaultProvider.apply(sender);
//    }


    // Logic

    public abstract @NotNull ParseResult2<T> parse(@NotNull CommandSender sender, @NotNull StringReader reader);

    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        // No suggestions returned by default
    }


    // Result factories

    protected @NotNull ParseResult2<T> partial() {
        return new ParseResult2.Partial<>();
    }

    protected @NotNull ParseResult2<T> success(@UnknownNullability T value) {
        return new ParseResult2.Success<>(value);
    }

    protected @NotNull ParseResult2<T> syntaxError() {
        return new ParseResult2.Failure<>(-1);
    }

    protected @NotNull ParseResult2<T> syntaxError(int start) {
        return new ParseResult2.Failure<>(start);
    }


}
