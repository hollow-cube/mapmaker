package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import net.hollowcube.mapmaker.util.NumberUtil;
import net.kyori.adventure.text.Component;
import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;

import static net.kyori.adventure.text.Component.text;
import static net.kyori.adventure.text.event.HoverEvent.showText;

@RuntimeGson
public record Leaderboard(
    boolean asc,
    @NotNull Format format,
    @NotNull String score
) {
    public enum Format {
        TIME,
        NUMBER,
        PERCENT,
        ;

        private static final DecimalFormat NUMBER_FULL_PRECISION = new DecimalFormat("#,##0.###############");

        public String missingText() {
            return switch (this) {
                case TIME -> "--:--:--";
                case NUMBER, PERCENT -> "-";
            };
        }

        public Component format(double value) {
            var formatted = text(formatPlain(value));
            if (this == NUMBER) return formatted.hoverEvent(showText(text(NUMBER_FULL_PRECISION.format(value))));
            return formatted;
        }

        public String formatPlain(double value) {
            return switch (this) {
                case TIME -> NumberUtil.formatMapPlaytime((long) value, true);
                case NUMBER -> NumberUtil.formatNumberTiered(value);
                case PERCENT -> NumberUtil.format(Math.clamp(value, 0, 100), 2) + "%";
            };
        }

    }

    public static final Leaderboard DEFAULT = new Leaderboard(true, Format.TIME, "q.playtime");

    public Leaderboard withAsc(boolean asc) {
        return new Leaderboard(asc, this.format, this.score);
    }

    public Leaderboard withFormat(Format format) {
        return new Leaderboard(this.asc, format, this.score);
    }

    public Leaderboard withScore(String score) {
        return new Leaderboard(this.asc, this.format, score);
    }
}
