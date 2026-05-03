package net.hollowcube.mapmaker.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public class MapArgument extends Argument<@Nullable MapData> {

    private final MapClient maps;

    protected MapArgument(@NotNull String id, MapClient maps) {
        super(id);
        this.maps = maps;
    }

    @Override
    public @NotNull ParseResult<@Nullable MapData> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        String mapId = reader.readWord(WordType.BRIGADIER);
        if (!(sender instanceof Player)) return syntaxError(-1);

        try {
            return getOrEmpty(() -> maps.get(mapId));
        } catch (IllegalArgumentException | MapService.NotFoundError ignored) {
            // Not a valid or existing map (endpoint checks both id and published id)
        }

        return new ParseResult.Failure<>(-1);
    }

    private static ParseResult.Success<@Nullable MapData> getOrEmpty(Supplier<MapData> supplier) {
        return new ParseResult.Success<>(() -> {
            try {
                return supplier.get();
            } catch (Throwable ignored) {
                return null;
            }
        });
    }

    @Override
    public boolean shouldSuggest() {
        return false;
    }
}
