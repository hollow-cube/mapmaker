package net.hollowcube.mapmaker.command.arg;

import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.arg.SuggestionEntry;
import net.hollowcube.command.arg.SuggestionResult;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.perm.PermManager;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.network.ConnectionManager;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CoreArgument {
    private static final ConnectionManager CONNECTION_MANAGER = MinecraftServer.getConnectionManager();

    // Player stuff

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

    // Map Stuff

    /**
     * EditableMap returns an argument for an editable map according to the player context.
     *
     * <p>This means for somebody without special permissions, one of their map slots, or a map id that they
     * own/have access to which is not published.</p>
     *
     * @param id
     * @param mapService
     * @param permManager
     * @return
     */
    public static @NotNull Argument<MapData> EditableMap(
            @NotNull String id,
            @NotNull MapService mapService,
            @NotNull PermManager permManager
    ) {
        return Argument.Word(id).map(
                /* Mapper */ (sender, raw) -> {
                    return new Argument.ParseSuccess<>(null);
                },
                /* Suggester */ (sender, reader, raw) -> {
//                    var results = new ArrayList<SuggestionEntry>();
//                    for (var result : playerService.getUsernameTabCompletions(raw).resultSafe()) {
//                        results.add(new SuggestionEntry(result.username(), null));
//                    }
                    return new SuggestionResult.Success(0, 0, List.of());
                }
        );
    }

    public static @NotNull Argument<MapData> AnyMap(
            @NotNull String id,
            @NotNull MapService mapService,
            @NotNull PermManager permManager
    ) {
        return Argument.Word(id).map(
                /* Mapper */ (sender, raw) -> {
                    return new Argument.ParseSuccess<>(null);
                },
                /* Suggester */ (sender, reader, raw) -> {
//                    var results = new ArrayList<SuggestionEntry>();
//                    for (var result : playerService.getUsernameTabCompletions(raw).resultSafe()) {
//                        results.add(new SuggestionEntry(result.username(), null));
//                    }
                    return new SuggestionResult.Success(0, 0, List.of());
                }
        );
    }

    private CoreArgument() {
    }
}
