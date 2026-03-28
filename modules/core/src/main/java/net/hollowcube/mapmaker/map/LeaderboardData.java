package net.hollowcube.mapmaker.map;


import net.hollowcube.common.util.FontUtil;
import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.api.players.PlayerClient;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.Component.text;

@RuntimeGson
public record LeaderboardData(
    @NotNull List<Entry> top,
    @Nullable Entry player
) {
    private static final TextColor COLOR_GOLD = TextColor.color(0xFFBC0F);
    private static final TextColor COLOR_SILVER = TextColor.color(0x808080);
    private static final TextColor COLOR_BRONZE = TextColor.color(0xCD7F32);
    private static final TextColor COLOR_DEFAULT = TextColor.color(0x696969);

    @RuntimeGson
    public record Entry(@NotNull String player, long score, int rank) {
    }

    public boolean contains(@NotNull String playerId) {
        return top().stream().anyMatch(e -> e.player().equals(playerId));
    }

    public long getScore(@NotNull String playerId) {
        return top().stream().filter(e -> e.player().equals(playerId)).findFirst().map(Entry::score).orElse(-1L);
    }

    public int getRank(@NotNull String playerId) {
        return top().stream().filter(e -> e.player().equals(playerId)).findFirst().map(Entry::rank).orElse(-1);
    }

    /**
     * Converts the leaderboard data to a sendable list of components for a leaderboard.
     *
     * @param pad True to pad the leaderboard with empty lines
     * @return Entry text, or null if the leaderboard is empty.
     */
    @Blocking
    public @Nullable List<Component> toComponents(@NotNull PlayerClient players, Leaderboard.Format lbFormat, boolean pad) {
        if (top().isEmpty()) return null;

        Component[] displayNames = new Component[top().size()];
        for (var i = 0; i < top().size(); i++) {
            displayNames[i] = players.getDisplayName(top().get(i).player()).build();
        }

        var result = new ArrayList<Component>();

        int[] nameWidths = new int[top().size()];

        int maxNumWidth = 0;
        int maxNameWidth = 0;
        for (var i = 0; i < top().size(); i++) {
            var entry = top().get(i);
            maxNumWidth = Math.max(maxNumWidth, FontUtil.measureText(String.format("#%d ", entry.rank())));

            nameWidths[i] = FontUtil.measureText(displayNames[i]);
            maxNameWidth = Math.max(maxNameWidth, nameWidths[i] + FontUtil.measureText(" "));
        }

        var selfId = player() != null ? player().player() : null;

        var shouldShowSelf = true;
        for (var i = 0; i < top().size(); i++) {
            var entry = top().get(i);
            var comp = text();
            var t = "#" + entry.rank();
            comp.append(text(t + FontUtil.computeOffset(maxNumWidth - FontUtil.measureText(t) + 4), switch (entry.rank()) {
                case 1 -> COLOR_GOLD;
                case 2 -> COLOR_SILVER;
                case 3 -> COLOR_BRONZE;
                default -> COLOR_DEFAULT;
            }));


            comp.append(displayNames[i])
                .append(text(FontUtil.computeOffset(maxNameWidth - nameWidths[i])))
                .appendSpace()
                .append(lbFormat.format(entry.score()).color(TextColor.color(0xf2f2f2)));

            result.add(comp.build());

            if (entry.player().equals(selfId))
                shouldShowSelf = false;
        }
        for (var i = top().size(); pad && i < 10; i++) {
            result.add(text(""));
        }

        if (shouldShowSelf && player() != null) {
            result.add(text("Your time: ").append(lbFormat.format(player().score())).append(text(" (#" + player().rank() + ")")));
        }

        return result;
    }
}
