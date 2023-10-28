package net.hollowcube.command.arg;

import net.hollowcube.command.CommandExecutor;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ArgumentOptional<T> extends Argument<T> {
    private final Argument<T> arg;

    public ArgumentOptional(@NotNull Argument<T> arg) {
        super(arg.id());
        this.arg = arg;
    }

    @Override
    public @Nullable CommandExecutor errorHandler() {
        return super.errorHandler() == null ? arg.errorHandler() : super.errorHandler();
    }

    @Override
    public @Nullable String description() {
        return super.description() == null ? arg.description() : super.description();
    }

    @Override
    public @Nullable String defaultName() {
        return super.defaultName() == null ? arg.defaultName() : super.defaultName();
    }

    @Override
    public boolean isOptional() {
        return true;
    }

    @Override
    public @NotNull ParseResult<T> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        if (!reader.canRead()) return new ParsePartial<>();
        return arg.parse(sender, reader);
    }

    @Override
    public void suggestions(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull Suggestion suggestion) {
        arg.suggestions(sender, reader, suggestion);
    }

    @Override
    public String toString() {
        return String.format("opt[%s]", arg.toString());
    }
}
