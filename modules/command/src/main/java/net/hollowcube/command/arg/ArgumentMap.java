package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiFunction;

public class ArgumentMap<S, T> extends Argument<T> {

    @FunctionalInterface
    public interface Suggester<T> {
        @NotNull SuggestionResult suggestions(@NotNull CommandSender sender, @NotNull StringReader reader, T value);
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
    public @NotNull SuggestionResult suggestions(@NotNull CommandSender sender, @NotNull StringReader reader) {
        // If no suggester is provided, immediately delegate to the source argument
        if (suggester == null) return source.suggestions(sender, reader);

        // If there is a suggester, we need to parse the source first.
        var result = source.parse(sender, reader);
        if (result instanceof Argument.ParseSuccess<S> success)
            return suggester.suggestions(sender, reader, success.value());
        return new SuggestionResult.Failure();
    }
}
