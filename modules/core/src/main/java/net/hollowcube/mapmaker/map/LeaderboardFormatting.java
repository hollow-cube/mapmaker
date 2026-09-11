package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.FontUtil;
import net.hollowcube.ipc.map.LeaderboardData;
import net.hollowcube.ipc.map.MapLeaderboard;
import net.hollowcube.ipc.player.PlayerService;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.Nullable;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.HoverEvent.showText;

public final class LeaderboardFormatting {
    private static final TextColor COLOR_GOLD = TextColor.color(0xFFBC0F);
    private static final TextColor COLOR_SILVER = TextColor.color(0x808080);
    private static final TextColor COLOR_BRONZE = TextColor.color(0xCD7F32);
    private static final TextColor COLOR_DEFAULT = TextColor.color(0x696969);

    /// The board as chat lines, one per entry and a last one for the player when they are not in
    /// the top; null when the board is empty.
    ///
    /// @param pad true to pad the leaderboard with empty lines
    @Blocking
    public static @Nullable List<Component> lines(LeaderboardData leaderboard, PlayerService players, MapLeaderboard.Format format, boolean pad) {
        var top = leaderboard.top();
        if (top.isEmpty()) return null;

        var ids = top.stream().map(LeaderboardData.Entry::player).toList();
        var names = players.displayNames(ids);
        Component[] displayNames = new Component[top.size()];
        for (var i = 0; i < top.size(); i++) {
            displayNames[i] = names.get(ids.get(i)).render();
        }

        var result = new ArrayList<Component>();

        int[] nameWidths = new int[top.size()];

        int maxNumWidth = 0;
        int maxNameWidth = 0;
        for (var i = 0; i < top.size(); i++) {
            var entry = top.get(i);
            maxNumWidth = Math.max(maxNumWidth, FontUtil.measureText(String.format("#%d ", entry.rank())));

            nameWidths[i] = FontUtil.measureText(displayNames[i]);
            maxNameWidth = Math.max(maxNameWidth, nameWidths[i] + FontUtil.measureText(" "));
        }

        var self = leaderboard.player();

        var shouldShowSelf = true;
        for (var i = 0; i < top.size(); i++) {
            var entry = top.get(i);
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
                .append(format(format, entry.score()).color(TextColor.color(0xf2f2f2)));

            result.add(comp.build());

            if (self != null && entry.player().equals(self.player()))
                shouldShowSelf = false;
        }
        for (var i = top.size(); pad && i < 10; i++) {
            result.add(text(""));
        }

        if (shouldShowSelf && self != null) {
            result.add(text("Your time: ").append(format(format, self.score())).append(text(" (#" + self.rank() + ")")));
        }

        return result;
    }

    public static String missingText(MapLeaderboard.Format format) {
        return format == MapLeaderboard.Format.TIME ? "--:--:--" : "-";
    }

    public static Component format(MapLeaderboard.Format format, double value) {
        var text = text(formatPlain(format, value));
        return format == MapLeaderboard.Format.NUMBER
            ? text.hoverEvent(
                showText(text(new DecimalFormat("#,##0.###############").format(value)))
            )
            : text;
    }

    public static String formatPlain(MapLeaderboard.Format format, double value) {
        return switch (format) {
            case TIME -> NumberUtil.formatMapPlaytime((long) value, true);
            case NUMBER -> NumberUtil.formatNumberTiered(value);
            case PERCENT -> NumberUtil.format(Math.clamp(value, 0, 100), 2) + "%";
            case UNKNOWN -> "-";
        };
    }

    private LeaderboardFormatting() {}
}
