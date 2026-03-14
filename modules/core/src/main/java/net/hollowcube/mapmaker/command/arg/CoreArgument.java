package net.hollowcube.mapmaker.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;

public final class CoreArgument {

    // Player stuff

    public static Argument<PlayerData> AnyPlayerData(String id, PlayerService playerService) {
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

    public static Argument<@Nullable String> AnyPlayerId(String id, PlayerService playerService) {
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

    public static Argument<@Nullable String> AnyOnlinePlayer(String id, SessionManager sessionManager) {
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

    public static MapArgument Map(String id, MapService mapService) {
        return new MapArgument(id, mapService);
    }

    public static MessageArgument Message(String id) {
        return new MessageArgument(id);
    }

    public static MapSettingArgument MapSetting(String id) {
        return new MapSettingArgument(id);
    }

    public static JsonArgument Json(String id) {
        return new JsonArgument(id);
    }

    private CoreArgument() {
    }
}
