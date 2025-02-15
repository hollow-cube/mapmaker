package net.hollowcube.command.arg;

import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public class ArgumentMap<S, T> extends Argument<T> {

    @FunctionalInterface
    public interface ParseFunc<S, T> {
        @NotNull
        ParseResult<T> parse(@NotNull CommandSender sender, @UnknownNullability S raw);
    }

    @FunctionalInterface
    public interface SuggestFunc {
        void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion);
    }


    private final Argument<S> source;
    private final ParseFunc<S, T> parseFunc;
    private final SuggestFunc suggestFunc;

    ArgumentMap(
            @NotNull String id, @NotNull Argument<S> source,
            @NotNull ParseFunc<S, T> parseFunc, @Nullable SuggestFunc suggestFunc
    ) {
        super(id);
        this.source = source;
        this.parseFunc = parseFunc;
        this.suggestFunc = suggestFunc;
    }

    @Override
    public @NotNull ParseResult<T> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        return switch (source.parse(sender, reader)) {
            //todo it is probably not great to have the get call here in case the prior argument is a deferred success
            case ParseResult.Success<S> success -> parseFunc.parse(sender, success.valueFunc().get());
            case ParseResult.Partial<S> partial -> partial(partial.message());
            case ParseResult.Failure<S> failure -> syntaxError(failure.start(), failure.message());
        };
    }

    @Override
    public void suggest(@NotNull CommandSender sender, @NotNull String raw, @NotNull Suggestion suggestion) {
        if (suggestFunc == null) {
            source.suggest(sender, raw, suggestion);
            return;
        }

        suggestFunc.suggest(sender, raw, suggestion);
    }
}
