package net.hollowcube.mapmaker.command.playerinfo;

import net.hollowcube.command.CommandContext;
import net.hollowcube.command.arg.Argument;
import net.hollowcube.command.dsl.CommandDsl;
import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.command.arg.CoreArgument;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.map.PlayerTopTimeEntry;
import net.hollowcube.mapmaker.map.responses.PlayerTopTimesResponse;
import net.hollowcube.mapmaker.player.PlayerService;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.TextColor;
import net.minestom.server.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.hollowcube.mapmaker.util.NumberUtil.formatMapPlaytime;

public class TopTimesInfoType extends CommandDsl {

    private final Argument<String> targetArgument;
    private final Argument<Integer> pageArgument;

    private final MapService mapService;
    private final PlayerService playerService;

    public TopTimesInfoType(MapService mapService, PlayerService playerService) {
        super("top_times");
        this.mapService = mapService;
        this.playerService = playerService;

        this.targetArgument = CoreArgument.AnyPlayerId("target", playerService);
        this.pageArgument = Argument.Int("page").min(1).defaultValue(1);

        this.addSyntax(this::exec, this.targetArgument);
        this.addSyntax(this::exec, this.targetArgument, this.pageArgument);
    }

    private void exec(@NotNull CommandSender sender, @NotNull CommandContext commandContext) {
        String targetId = commandContext.get(this.targetArgument);
        int page = commandContext.get(this.pageArgument);

        if (targetId == null) {
            sender.sendMessage(Component.text("Player not found"));
            return;
        }
        Component targetName = this.playerService.getPlayerDisplayName2(targetId).asComponent();

        PlayerTopTimesResponse resp = this.mapService.getPlayerTopTimes(targetId, page, 15);
        int maxPage = Math.ceilDiv(resp.totalItems(), 15);
        if (maxPage == 0) {
            sender.sendMessage(targetName.append(Component.text(" has no top times")));
            return;
        }

        if (page > maxPage) {
            page = maxPage;
            resp = this.mapService.getPlayerTopTimes(targetId, page, 15);
        }

        sender.sendMessage(targetName.append(
            Component.text("'s Top Times (%s/%s):".formatted(page, Math.ceilDiv(resp.totalItems(), 15)))));
        List<LeaderboardData.Entry> entries = new ArrayList<>();
        for (PlayerTopTimeEntry entry : resp.items()) {
            entries.add(
                new LeaderboardData.Entry(entry.mapName(), entry.publishedId(), entry.completionTime(), entry.rank()));
        }
        LeaderboardData leaderboardData = new LeaderboardData(entries);
        leaderboardData.toComponents().forEach(sender::sendMessage);
    }

    /**
     * This is a modified version of the LeaderboardData in the map service responses that is slimmed down for this purpose
     * and removes username resolution.
     */
    public record LeaderboardData(
        @NotNull List<Entry> top
    ) {
        private static final TextColor COLOR_GOLD = TextColor.color(0xFFBC0F);
        private static final TextColor COLOR_SILVER = TextColor.color(0x808080);
        private static final TextColor COLOR_BRONZE = TextColor.color(0xCD7F32);
        private static final TextColor COLOR_DEFAULT = TextColor.color(0x696969);

        @RuntimeGson
        public record Entry(@NotNull String mapName, int publishedMapId, long completionTime, int rank) {
        }

        /**
         * Converts the leaderboard data to a sendable list of components for a leaderboard.
         *
         * @return Entry text, or null if the leaderboard is empty.
         */
        public @Nullable List<Component> toComponents() {
            if (top().isEmpty()) return null;

            var result = new ArrayList<Component>();

            int[] nameWidths = new int[top().size()];

            int maxNumWidth = 0;
            int maxNameWidth = 0;
            for (var i = 0; i < top().size(); i++) {
                var entry = top().get(i);
                maxNumWidth = Math.max(maxNumWidth, FontUtil.measureText(String.format("#%d ", entry.rank())));

                nameWidths[i] = FontUtil.measureText(entry.mapName());
                maxNameWidth = Math.max(maxNameWidth, nameWidths[i] + FontUtil.measureText(" "));
            }

            for (var i = 0; i < top().size(); i++) {
                var entry = top().get(i);
                var comp = Component.text();
                var t = "#" + entry.rank();
                comp.append(Component.text(t + FontUtil.computeOffset(maxNumWidth - FontUtil.measureText(t) + 4),
                                           switch (entry.rank()) {
                                               case 1 -> COLOR_GOLD;
                                               case 2 -> COLOR_SILVER;
                                               case 3 -> COLOR_BRONZE;
                                               default -> COLOR_DEFAULT;
                                           }));

                comp.append(
                        Component.text(entry.mapName())
                            .hoverEvent(Component.text("Click to copy published ID"))
                            .clickEvent(ClickEvent.copyToClipboard(MapData.formatPublishedId(entry.publishedMapId())))
                    ).append(Component.text(FontUtil.computeOffset(maxNameWidth - nameWidths[i])))
                    .append(Component.text(" " + formatMapPlaytime(entry.completionTime(), true),
                                           TextColor.color(0xf2f2f2)));

                result.add(comp.build());
            }

            return result;
        }
    }


}
