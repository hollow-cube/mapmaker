package net.hollowcube.command.argold;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public class ArgumentMap<S, T> extends Argument<T> {

    @FunctionalInterface
    public interface Suggester<T> {
        void suggestions(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull Suggestion suggestion, T value);
    }

    private final Argument<S> source;
    private final BiFunction<CommandSender, S, ParseResult<T>> mapper;
    private final Suggester<S> suggester;

    ArgumentMap(
            @NotNull String id,
            @NotNull Argument<S> source,
            @NotNull BiFunction<CommandSender, S, ParseResult<T>> mapper,
            @Nullable Suggester<S> suggester
    ) {
        super(id);
        this.source = source;
        this.mapper = mapper;
        this.suggester = suggester;
    }

    @Override
    public @NotNull ParseResult<T> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        var result = source.parse(sender, reader);
        if (result instanceof ParseSuccess) {
            return mapper.apply(sender, ((ParseSuccess<S>) result).value());
        } else if (result instanceof ParsePartial) {
            return new ParsePartial<>();
        } else {
            return new ParseFailure<>();
        }
    }

    @Override
    public void suggestions(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull Suggestion suggestion) {
        // If no suggester is provided, immediately delegate to the source argument
        if (suggester == null) {
            source.suggestions(sender, reader, suggestion);
            return;
        }

        // If there is a suggester, we need to parse the source first.
        var result = source.parse(sender, reader);
        if (result instanceof Argument.ParseSuccess<S> success) //todo this will not work with deferred success
            suggester.suggestions(sender, reader, suggestion, success.value());
    }
}
