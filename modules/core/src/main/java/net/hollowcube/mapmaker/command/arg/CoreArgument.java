package net.hollowcube.mapmaker.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.SuggestionEntry;
import net.hollowcube.command.arg.SuggestionResult;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.ConnectionManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.UUID;

public final class CoreArgument {
    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    public static @NotNull Argument<PlayerDataV2> AnyPlayerData(@NotNull String id, @NotNull PlayerService playerService) {
        return Argument.Word(id).map(
                /* Mapper */ (sender, raw) -> {
                    //todo
                    var pd = new PlayerDataV2(UUID.randomUUID().toString(), raw, Component.text(raw));
                    return new Argument.ParseSuccess<>(pd);
                },
                /* Suggester */ (sender, reader, raw) -> {
                    var results = new ArrayList<SuggestionEntry>();
                    for (var result : playerService.getUsernameTabCompletions(raw).resultSafe()) {
                        results.add(new SuggestionEntry(result.username(), null));
                    }
                    return new SuggestionResult.Success(reader.pos() - raw.length(), raw.length(), results);
                }
        );
    }

    public static @NotNull Argument<String> AnyPlayerId(@NotNull String id, @NotNull PlayerService playerService) {
        return Argument.Word(id).map(
                /* Mapper */ (sender, raw) -> new Argument.ParseSuccess<>(raw),
                /* Suggester */ (sender, reader, raw) -> {
                    var results = new ArrayList<SuggestionEntry>();
                    for (var result : playerService.getUsernameTabCompletions(raw).resultSafe()) {
                        results.add(new SuggestionEntry(result.username(), null));
                    }
                    return new SuggestionResult.Success(reader.pos() - raw.length(), raw.length(), results);
                }
        );
    }

    private CoreArgument() {
    }
}
