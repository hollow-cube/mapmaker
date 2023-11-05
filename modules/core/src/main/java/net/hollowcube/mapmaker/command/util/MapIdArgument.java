package net.hollowcube.mapmaker.command.util;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.suggestion.Suggestion;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class MapIdArgument extends Argument<String> {
    private final MapService mapService;

    MapIdArgument(@NotNull String id, @NotNull MapService mapService) {
        super(id);
        this.mapService = mapService;
    }

    @Override
    public @NotNull ParseResult<String> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        return new ParseSuccess<>(reader.readRemaining());
    }

    @Override
    public void suggestions(@NotNull CommandSender sender, @NotNull StringReader reader, @NotNull Suggestion suggestion) {
        var text = reader.readRemaining();
        suggestion.add(UUID.randomUUID().toString(), Component.text("My Map Name").appendNewline().append(Component.text("My Map Description")));
    }
}
