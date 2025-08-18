package net.hollowcube.mapmaker.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.command.util.StringReader;
import net.hollowcube.command.util.WordType;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerData;
import net.minestom.server.command.CommandSender;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

public class MapArgument extends Argument<@Nullable MapData> {

    private final MapService mapService;

    protected MapArgument(@NotNull String id, MapService mapService) {
        super(id);
        this.mapService = mapService;
    }

    @Override
    public @NotNull ParseResult<@Nullable MapData> parse(@NotNull CommandSender sender, @NotNull StringReader reader) {
        String mapId = reader.readWord(WordType.BRIGADIER);
        if (!(sender instanceof Player player)) return syntaxError(-1);

        var playerId = PlayerData.fromPlayer(player).id();

        try {
            // Try as published ID
            var publishedId = MapData.parsePublishedID(mapId);
            return getOrEmpty(() -> this.mapService.getMapByPublishedId(playerId, publishedId));
        } catch (IllegalArgumentException | MapService.NotFoundError ignored) {
            // Not a valid published ID
        }

        try {
            // Try as a full ID
            var fullId = UUID.fromString(mapId).toString();
            return getOrEmpty(() -> mapService.getMap(playerId, fullId));
        } catch (IllegalArgumentException | MapService.NotFoundError ignored) {
            // Not a valid UUID
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
