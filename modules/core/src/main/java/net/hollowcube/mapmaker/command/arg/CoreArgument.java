package net.hollowcube.mapmaker.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
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
                    for (var session : sessionManager.sessions(false)) {
                        if (session.username().equalsIgnoreCase(raw) || session.playerId().equalsIgnoreCase(raw)) {
                            return session.playerId();
                        }
                    }
                    return null;
                }),
                /* Suggester */ (sender, raw, suggestion) -> {
                    raw = raw.toLowerCase(Locale.ROOT);
                    for (var session : sessionManager.sessions(false)) {
                        if (session.username().toLowerCase(Locale.ROOT).startsWith(raw)) {
                            suggestion.add(session.username());
                        }
                    }
                }
        );
    }

    // Map Stuff

    public static MapArgument Map(@NotNull String id, @NotNull MapService mapService) {
        return new MapArgument(id, mapService);
    }

    public static MessageArgument Message(@NotNull String id) {
        return new MessageArgument(id);
    }

    public static MapSettingArgument MapSetting(@NotNull String id) {
        return new MapSettingArgument(id);
    }

    public static JsonArgument Json(@NotNull String id) {
        return new JsonArgument(id);
    }

    private CoreArgument() {
    }
}
