package net.hollowcube.mapmaker.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.ParseResult;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.hollowcube.mapmaker.player.PlayerData;
import net.hollowcube.mapmaker.session.SessionManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;

public final class CoreArgument {

    // Player stuff

    public static @NotNull Argument<PlayerData> AnyPlayerData(
        @NotNull String id, @NotNull PlayerClient players
    ) {
        return Argument.Word(id).map(
            /* Mapper */ (sender, raw) -> new ParseResult.Success<>(() -> {
                if (raw.trim().isEmpty()) return null;
                try {
                    return players.getPlayerData(raw);
                } catch (ApiClient.NotFoundError ignored) {
                    return null;
                }
            }),
            /* Suggester */ (sender, raw, suggestion) -> {
                for (var result : players.searchPlayers(raw, List.of(), 15)) {
                    suggestion.add(result.displayName().getUsername());
                }
            }
        );
    }

    public static @NotNull Argument<@Nullable String> AnyPlayerId(
        @NotNull String id, @NotNull PlayerClient players
    ) {
        return Argument.Word(id).map(
            /* Mapper */ (sender, raw) -> new ParseResult.Success<>(() -> {
                if (raw.trim().isEmpty()) return null;
                try {
                    return players.getPlayerData(raw).id();
                } catch (ApiClient.NotFoundError ignored) {
                    return null;
                }
            }),
            /* Suggester */ (sender, raw, suggestion) -> {
                for (var result : players.searchPlayers(raw, List.of(), 15)) {
                    suggestion.add(result.displayName().getUsername());
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
