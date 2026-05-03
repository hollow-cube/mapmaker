package net.hollowcube.mapmaker.command;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.mapmaker.api.ApiClient;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.misc.MiscFunctionality;
import net.hollowcube.mapmaker.player.PlayerData;
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

    private final ApiClient api;
    private final MapService maps;
    private final SessionManager sessions;

    public TopTimesCommand(@NotNull ApiClient api, @NotNull MapService maps, @NotNull SessionManager sessions) {
        super("toptimes", "tt", "leaderboard", "lb");
        this.api = api;
        this.maps = maps;
        this.sessions = sessions;

        category = CommandCategories.MAP;
        description = "Shows the top leaderboard positions for a map";

        mapArg = CoreArgument.Map("map", api.maps).description("The map to check the leaderboard of");

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
            map = MiscFunctionality.getCurrentMap(sessions, api.maps, player);
            if (map == null) {
                player.sendMessage(NO_MAP_PLAYED);
                return;
            }
        }

        if (map.settings().getVariant() != MapVariant.PARKOUR) {
            player.sendMessage(Component.translatable(MAP_CANT_HAVE_TIMES, Component.text(map.id())));
        } else {
            var playerData = PlayerData.fromPlayer(player);
            var leaderboard = maps.getPlaytimeLeaderboard(map.id(), playerData.id());

            // TODO: we have to fetch the map from v4 api to get the leaderboard config, should port everything here to new api.
            var lbFormat = api.maps.get(map.id()).settings().leaderboard().format();
            var messages = leaderboard.toComponents(api.players, lbFormat, false);

            if (messages == null) {
                player.sendMessage(Component.translatable(NO_TIMES_FOUND, Component.text(map.id())));
            } else {
                messages.forEach(player::sendMessage);
            }
        }
    }
}
