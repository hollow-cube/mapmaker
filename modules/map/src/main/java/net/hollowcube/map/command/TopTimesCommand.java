package net.hollowcube.map.command;

import net.hollowcube.common.lang.GenericMessages;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.map.world.MapWorld;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.player.PlayerDataV2;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.minestom.server.command.CommandSender;
import net.minestom.server.command.builder.CommandContext;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

public class TopTimesCommand extends BaseMapCommand {
    private final PlayerService playerService;
    private final MapService mapService;

    public TopTimesCommand(@NotNull PlayerService playerService, @NotNull MapService mapService) {
        super("toptimes", "tt");
        this.playerService = playerService;
        this.mapService = mapService;

        setDefaultExecutor(this::showTopTimes);
    }

    private void showTopTimes(@NotNull CommandSender sender, @NotNull CommandContext unused) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(GenericMessages.COMMAND_PLAYER_ONLY);
            return;
        }

        var world = MapWorld.forPlayer(player);
        //todo should not run for non-map worlds

        var playerData = PlayerDataV2.fromPlayer(player);
        var leaderboard = mapService.getPlaytimeLeaderboard(world.map().id(), playerData.id());

        if (leaderboard.top().isEmpty()) {
            player.sendMessage("No times have been recorded yet.");
            return;
        }

        int[] nameWidths = new int[leaderboard.top().size()];

        int maxNumWidth = 0;
        int maxNameWidth = 0;
        for (var i = 0; i < leaderboard.top().size(); i++) {
            var entry = leaderboard.top().get(i);
            maxNumWidth = Math.max(maxNumWidth, FontUtil.measureText(String.format("#%d ", entry.rank())));

            var playerName = playerService.getPlayerDisplayName(entry.player());
            var playerNameText = PlainTextComponentSerializer.plainText().serialize(playerName);
            nameWidths[i] = FontUtil.measureText(playerNameText);
            maxNameWidth = Math.max(maxNameWidth, nameWidths[i] + FontUtil.measureText(" "));
//            var length = playerName.content().length();
//            if (length > maxWidth) maxWidth = length;
        }

        var shouldShowSelf = true;
        for (var i = leaderboard.top().size() - 1; i >= 0; i--) {
            var entry = leaderboard.top().get(i);
            var comp = Component.text();
            var t = "#" + entry.rank();
            comp.append(Component.text(t + FontUtil.computeOffset(maxNumWidth - FontUtil.measureText(t) + 4), TextColor.color(0x696969)));

            var playerName = playerService.getPlayerDisplayName(entry.player());
            comp.append(playerName).append(Component.text(FontUtil.computeOffset(maxNameWidth - nameWidths[i])));
            comp.append(Component.text(" " + timeToFriendly(entry.score()), TextColor.color(0xf2f2f2)));

//            player.sendMessage("#" + (entry.rank()) + entry.player() + ": " + entry.score() + "ms");

            player.sendMessage(comp.build());

            if (playerData.id().equals(entry.player()))
                shouldShowSelf = false;
        }

        var playerEntry = leaderboard.player();
        if (shouldShowSelf && playerEntry != null) {
            player.sendMessage("Your time: " + playerEntry.score() + "ms (#" + playerEntry.rank() + ")");
        }
    }

    private static @NotNull String timeToFriendly(long timeInMs) {
        var result = new StringBuilder();
        var hours = timeInMs / 3600000;
        if (hours > 0) {
            result.append(hours).append("h ");
            timeInMs %= 3600000;
        }

        var minutes = timeInMs / 60000;
        if (minutes > 0) {
            result.append(minutes).append("m ");
            timeInMs %= 60000;
        }

        var seconds = timeInMs / 1000;
        if (seconds > 0) {
            result.append(seconds).append("s ");
            timeInMs %= 1000;
        }

        if (timeInMs > 0) {
            result.append(timeInMs).append("ms");
        }

        return result.toString();
    }
}
