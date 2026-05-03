package net.hollowcube.mapmaker.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class CoreArgument {

    // Player stuff

    public static @NotNull Argument<PlayerData> AnyPlayerData(
        @NotNull String id, @NotNull PlayerService playerService) {
        return Argument.Word(id).map(
            /* Mapper */ (sender, raw) -> new ParseResult.Success<>(() -> {
                if (raw.trim().isEmpty()) return null;
                try {
                    // todo would be nice to combine into a single req in the future (api side)
                    String playerId = playerService.getPlayerId(raw);
                    return playerService.getPlayerData(playerId);
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

    public static @NotNull Argument<@Nullable String> AnyPlayerId(
        @NotNull String id, @NotNull PlayerClient players) {
        return Argument.Word(id).map(
            /* Mapper */ (sender, raw) -> new ParseResult.Success<>(() -> {
                if (raw.trim().isEmpty()) return null;
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

    public static @NotNull Argument<@Nullable String> AnyOnlinePlayer(
        @NotNull String id, @NotNull SessionManager sessionManager) {
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

    public static MapArgument Map(@NotNull String id, @NotNull MapClient maps) {
        return new MapArgument(id, maps);
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
