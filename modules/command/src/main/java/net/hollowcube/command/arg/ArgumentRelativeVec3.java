package net.hollowcube.command.arg;

import net.hollowcube.command.util.StringReader;
import net.minestom.server.command.CommandSender;
import net.minestom.server.coordinate.Point;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class ArgumentRelativeVec3 extends Argument<Point> {
    ArgumentRelativeVec3(@NotNull String id) {
        super(id);
    }

    @Override
    public @NotNull ParseResult<Point> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        return new ParseSuccess<>(null);
    }

    @Override
    public @NotNull SuggestionResult suggestions(@NotNull CommandSender sender, @NotNull StringReader reader) {
        return new SuggestionResult.Success(0, 0, List.of());
    }

    private double readCoordinate() {
        return 0;
    }

}
