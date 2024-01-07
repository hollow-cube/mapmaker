package net.hollowcube.command.argold;

import net.hollowcube.command.CommandExecutor;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;
import java.util.function.Function;

public abstract class Argument<T> {


    private final String id;
    private CommandExecutor errorHandler = null;
    private Function<CommandSender, T> defaultProvider = null;

    // Documentation bits
    private String description = null;
    private String defaultName = null;

    protected Argument(@NotNull String id) {
        this.id = id;
    }

    public @NotNull String id() {
        return id;
    }

    public @Nullable CommandExecutor errorHandler() {
        return errorHandler;
    }

    /**
     * An error handler is called when this argument is the final argument in a selected syntax
     * (typically because it is the last one), and the argument has either a partial or failed parse
     * AND the pass is execution.
     *
     * <p>Basically, if the last argument is a failure or partial result, this executor will be called.</p>
     *
     * @param errorHandler The error handler to execute
     * @return this
     */
    public @NotNull Argument<T> errorHandler(@Nullable CommandExecutor errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    public @NotNull Argument<T> doc(@NotNull String description) {
        this.description = description;
        return this;
    }

    public @NotNull Argument<T> doc(@NotNull String description, @Nullable String defaultName) {
        this.description = description;
        this.defaultName = defaultName;
        return this;
    }

    public @Nullable String description() {
        return description;
    }

    public @Nullable String defaultName() {
        return defaultName;
    }

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

    public <R> @NotNull Argument<R> map(@NotNull BiFunction<CommandSender, T, ParseResult<R>> mapper) {
        return new ArgumentMap<>(id, this, mapper, null);
    }

    public <R> @NotNull Argument<R> map(@NotNull BiFunction<CommandSender, T, ParseResult<R>> mapper, @NotNull ArgumentMap.Suggester<T> suggester) {
        return new ArgumentMap<>(id, this, mapper, suggester);
    }

    public abstract @NotNull ParseResult<T> parse(@NotNull CommandSender sender, @NotNull StringReader reader);

    public void suggestions(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull Suggestion suggestion) {
    }

    public sealed interface ParseResult<T> permits ParseSuccess, ParseDeferredSuccess, ParseFailure, ParsePartial {
    }

    public record ParseSuccess<T>(T value) implements ParseResult<T> {
    }

    public interface DeferredValue<T> {
        T get();
    }

    public record ParseDeferredSuccess<T>(DeferredValue<T> value) implements ParseResult<T> {
    }

    public record ParseFailure<T>() implements ParseResult<T> {
    }

    public record ParsePartial<T>() implements ParseResult<T> {
    }


}
