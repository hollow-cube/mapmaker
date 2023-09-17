package net.hollowcube.map.command;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.LeaderboardData;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.MapVariant;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.Command;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.command.builder.arguments.Argument;
import net.minestom.server.command.builder.arguments.ArgumentType;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class TopTimesCommand extends Command {
    private final PlayerService playerService;
    private final MapService mapService;
    private final Argument<UUID> mapIdArg = ArgumentType.UUID("map-id");

    public TopTimesCommand(@NotNull PlayerService playerService, @NotNull MapService mapService) {
        super("toptimes", "tt");
        setCondition(this::isAvailable);

        this.playerService = playerService;
        this.mapService = mapService;

        setDefaultExecutor(this::showTopTimes);
        addSyntax(this::showTopTimesOfMap, mapIdArg);
    }

    private boolean isAvailable(@NotNull CommandSender sender, @Nullable String unused) {
        if (!(sender instanceof Player player))
            return false;

        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return true; // The player is in the hub

        return world.map().settings().getVariant() == MapVariant.PARKOUR;
    }

    private void showTopTimes(@NotNull CommandSender sender, @NotNull CommandContext unused) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var playerData = MapPlayerData.fromPlayer(player);
        // TODO Find a better method for determining if the player is in the hub
        if (player.getInstance().getDimensionName().equals("mapmaker:hub")) {
            // Player is in hub, check their last played
            String mapId = playerData.lastPlayedMap();
            if (mapId == null || mapId.isBlank()) {
                player.sendMessage(Component.text("Unable to check last played map times.", NamedTextColor.RED));
            } else {
                var leaderboard = mapService.getPlaytimeLeaderboard(mapId, playerData.id());
                formatAndSendLeaderboardData(player, leaderboard);
            }
        } else {
            var world = MapWorld.forPlayer(player);
            var leaderboard = mapService.getPlaytimeLeaderboard(world.map().id(), playerData.id());
            formatAndSendLeaderboardData(player, leaderboard);
        }
    }


    private void showTopTimesOfMap(@NotNull CommandSender sender, @NotNull CommandContext context) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }
        var playerData = PlayerDataV2.fromPlayer(player);
        var mapId = context.get(mapIdArg).toString();
        LeaderboardData leaderboard = mapService.getPlaytimeLeaderboard(mapId, playerData.id());

        formatAndSendLeaderboardData(player, leaderboard);

//        if (leaderboard.top().isEmpty()) {
//            player.sendMessage("No times have been recorded yet.");
//            return;
//        }
//
//        int[] nameWidths = new int[leaderboard.top().size()];
//
//        int maxNumWidth = 0;
//        int maxNameWidth = 0;
//        for (var i = 0; i < leaderboard.top().size(); i++) {
//            var entry = leaderboard.top().get(i);
//            maxNumWidth = Math.max(maxNumWidth, FontUtil.measureText(String.format("#%d ", entry.rank())));
//
//            var playerName = playerService.getPlayerDisplayName(entry.player());
//            var playerNameText = PlainTextComponentSerializer.plainText().serialize(playerName);
//            nameWidths[i] = FontUtil.measureText(playerNameText);
//            maxNameWidth = Math.max(maxNameWidth, nameWidths[i] + FontUtil.measureText(" "));
////            var length = playerName.content().length();
////            if (length > maxWidth) maxWidth = length;
//        }
//
//        var shouldShowSelf = true;
//        for (var i = leaderboard.top().size() - 1; i >= 0; i--) {
//            var entry = leaderboard.top().get(i);
//            var comp = Component.text();
//            var t = "#" + entry.rank();
//            comp.append(Component.text(t + FontUtil.computeOffset(maxNumWidth - FontUtil.measureText(t) + 4), TextColor.color(0x696969)));
//
//            var playerName = playerService.getPlayerDisplayName(entry.player());
//            comp.append(playerName).append(Component.text(FontUtil.computeOffset(maxNameWidth - nameWidths[i])));
//            comp.append(Component.text(" " + timeToFriendly(entry.score()), TextColor.color(0xf2f2f2)));
//
////            player.sendMessage("#" + (entry.rank()) + entry.player() + ": " + entry.score() + "ms");
//
//            player.sendMessage(comp.build());
//
//            if (playerData.id().equals(entry.player()))
//                shouldShowSelf = false;
//        }
//
//        var playerEntry = leaderboard.player();
//        if (shouldShowSelf && playerEntry != null) {
//            player.sendMessage("Your time: " + playerEntry.score() + "ms (#" + playerEntry.rank() + ")");
//        }
    }

    private void formatAndSendLeaderboardData(@NotNull Player player, @NotNull LeaderboardData leaderboard) {

        var messages = leaderboard.toComponents(playerService, false);
        if (messages == null) {
            player.sendMessage("No times have been recorded yet.");
            return;
        }

        messages.forEach(player::sendMessage);
    }
//
//    private static @NotNull String timeToFriendly(long timeInMs) {
//        var result = new StringBuilder();
//        var hours = timeInMs / 3600000;
//        if (hours > 0) {
//            result.append(hours).append("h ");
//            timeInMs %= 3600000;
//        }
//
//        var minutes = timeInMs / 60000;
//        if (minutes > 0) {
//            result.append(minutes).append("m ");
//            timeInMs %= 60000;
//        }
//
//        var seconds = timeInMs / 1000;
//        if (seconds > 0) {
//            result.append(seconds).append("s ");
//            timeInMs %= 1000;
//        }
//
//        if (timeInMs > 0) {
//            result.append(timeInMs).append("ms");
//        }
//
//        return result.toString();
//    }
}
