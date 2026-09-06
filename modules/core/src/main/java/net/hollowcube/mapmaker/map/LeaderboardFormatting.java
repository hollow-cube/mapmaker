package net.hollowcube.mapmaker.map;

import net.hollowcube.ipc.map.MapLeaderboard;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;

import java.text.DecimalFormat;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.HoverEvent.showText;

public final class LeaderboardFormatting {
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
