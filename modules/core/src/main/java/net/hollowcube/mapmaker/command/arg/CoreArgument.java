package net.hollowcube.mapmaker.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

public final class CoreArgument {

    // Player stuff

    public static @NotNull Argument<PlayerDataV2> AnyPlayerData(@NotNull String id, @NotNull PlayerService playerService) {
        return Argument.Word(id).map(
                /* Mapper */ (sender, raw) -> {
                    //todo
                    var pd = new PlayerDataV2(UUID.randomUUID().toString(), raw, Component.text(raw));
                    return new ParseResult.Success<>(pd);
                },
                /* Suggester */ (sender, raw, suggestion) -> {
                    for (var result : playerService.getUsernameTabCompletions(raw).resultSafe()) {
                        suggestion.add(result.username());
                    }
                }
        );
    }

    public static @NotNull Argument<@Nullable String> AnyPlayerId(@NotNull String id, @NotNull PlayerService playerService) {
        return Argument.Word(id).map(
                /* Mapper */ (sender, raw) -> new ParseResult.Success<>(() -> {
                    try {
                        return playerService.getPlayerId(raw);
                    } catch (PlayerService.NotFoundError ignored) {
                        return null;
                    }
                }),
                /* Suggester */ (sender, raw, suggestion) -> {
                    for (var result : playerService.getUsernameTabCompletions(raw).resultSafe()) {
                        suggestion.add(result.username());
                    }
                }
        );
    }

    public static @NotNull Argument<@Nullable String> AnyOnlinePlayer(@NotNull String id, @NotNull SessionManager sessionManager) {
        return Argument.Word(id).map(
                /* Mapper */ (sender, raw) -> new ParseResult.Success<>(() -> {
                    for (var session : sessionManager.sessions()) {
                        if (session.username().equalsIgnoreCase(raw)) {
                            return session.playerId();
                        }
                    }
                    return null;
                }),
                /* Suggester */ (sender, raw, suggestion) -> {
                    raw = raw.toLowerCase(Locale.ROOT);
                    for (var session : sessionManager.sessions()) {
                        if (session.username().toLowerCase(Locale.ROOT).startsWith(raw)) {
                            suggestion.add(session.username());
                        }
                    }
                }
        );
    }

    // Map Stuff

    /**
     * PlayableMap returns an argument for a playable map according to the player context.
     *
     * @param id
     * @param mapService
     * @return
     */
    public static @NotNull Argument<MapData> PlayableMap(
            @NotNull String id,
            @NotNull MapService mapService
    ) {
        return Argument.Word(id).map(
                /* Mapper */ (sender, raw) -> {
                    if (!(sender instanceof Player player))
                        return new ParseResult.Failure<>(-1);
                    var playerData = PlayerDataV2.fromPlayer(player);

                    try { // Try as published ID
                        var publishedId = MapData.parsePublishedID(raw);
                        return new ParseResult.Success<>(() -> mapService.getMapByPublishedId(playerData.id(), publishedId));
                    } catch (IllegalArgumentException ignored) {
                        // Not a valid published ID
                    }

                    try { // Try as a full ID
                        var fullId = UUID.fromString(raw).toString();
                        return new ParseResult.Success<>(() -> mapService.getMap(playerData.id(), fullId));
                    } catch (IllegalArgumentException ignored) {
                        // Not a valid UUID
                    }

                    // Not a valid id so always fail
                    return new ParseResult.Failure<>(-1);
                },
                /* Suggester */ (sender, raw, suggestion) ->
                        // No suggestions, it is a published map so either the full id or published id.
                        // Either way not likely being typed out manually.
                {
                }
        );
    }

    private CoreArgument() {
    }
}
