package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.hollowcube.mapmaker.session.SessionManager;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TopTimesCommand extends CommandDsl {

    private static final String NO_MAP_FOUND = "commands.top_times.no_map_found";
    private static final String NO_TIMES_FOUND = "commands.top_times.no_times_found";
    private static final String MAP_CANT_HAVE_TIMES = "commands.top_times.map_cant_have_top_times";
    private static final Component NO_MAP_PLAYED = Component.translatable("commands.top_times.no_maps_played");

    private final Argument<@Nullable MapData> mapArg;

    private final MapService maps;
    private final PlayerService players;
    private final SessionManager sessions;

    public TopTimesCommand(@NotNull MapService maps, @NotNull PlayerService players, @NotNull SessionManager sessions) {
        super("toptimes", "tt");
        this.maps = maps;
        this.players = players;
        this.sessions = sessions;

        category = CommandCategories.MAP;
        description = "Lists the top 10 fastest completion times of a map";

        mapArg = CoreArgument.Map("map", maps).description("The map to check the top times of");

        addSyntax(playerOnly(this::showTopTimes));
        addSyntax(playerOnly(this::showTopTimes), mapArg);
    }

    private void showTopTimes(@NotNull Player player, @NotNull CommandContext context) {
        MapData map;
        if (context.has(mapArg)) {
            map = context.get(mapArg);
            if (map == null) {
                player.sendMessage(Component.translatable(NO_MAP_FOUND, Component.text(context.getRaw(mapArg))));
                return;
            }
        } else {
            map = OpUtils.or(
                    MiscFunctionality.getCurrentMap(sessions, maps, player),
                    () -> OpUtils.map(
                            MapPlayerData.fromPlayer(player).lastPlayedMap(),
                            id -> maps.getMap(PlayerDataV2.fromPlayer(player).id(), id)
                    )
            );

            if (map == null) {
                player.sendMessage(NO_MAP_PLAYED);
                return;
            }
        }

        if (map.settings().getVariant() != MapVariant.PARKOUR) {
            player.sendMessage(Component.translatable(MAP_CANT_HAVE_TIMES, Component.text(map.id())));
        } else {
            var playerData = PlayerDataV2.fromPlayer(player);
            var leaderboard = maps.getPlaytimeLeaderboard(map.id(), playerData.id());
            var messages = leaderboard.toComponents(players, false);

            if (messages == null) {
                player.sendMessage(Component.translatable(NO_TIMES_FOUND, Component.text(map.id())));
            } else {
                messages.forEach(player::sendMessage);
            }
        }
    }
}
