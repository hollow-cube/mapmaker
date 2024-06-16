package net.hollowcube.mapmaker.util;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public final class NumberUtil {

    public static @NotNull String format(double number, int maxDecimalPlaces) {
        BigDecimal bigDecimal = new BigDecimal(Double.toString(number));
        bigDecimal = bigDecimal.setScale(maxDecimalPlaces, RoundingMode.DOWN);

        return bigDecimal.stripTrailingZeros().toPlainString();
    }

    /**
     * formatCurrency will take a number and format it as a currency string. The string is at most 5 characters long
     * with a max of 3 numbers, and an optional '.' and multiplier. The highest representable number is 999b, any number
     * higher than that will show 999b.
     *
     * <p>For example: 1.23k, 1, 11, 10k, 100m, 200b</p>
     */
    public static @NotNull String formatCurrency(long value) {
        if (value > 999_999_999_999L) return "999t";
        if (value > 999_999_999) return roundToThreeSigFigs(value / 1_000_000_000f, 'b');
        if (value > 999_999) return roundToThreeSigFigs(value / 1_000_000f, 'm');
        if (value > 999) return roundToThreeSigFigs(value / 1_000f, 'k');
        return String.valueOf(value);
    }

    private static @NotNull String roundToThreeSigFigs(double value, char suffix) {
        BigDecimal decimal = new BigDecimal(value, MathContext.DECIMAL64);
        return decimal.round(new MathContext(3, RoundingMode.HALF_UP)).stripTrailingZeros().toPlainString() + suffix;
    }

    public static @NotNull String formatMapPlaytime(long time, boolean roundToTicks) {
        long minutes = TimeUnit.MILLISECONDS.toMinutes(time);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(time) - TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(time));
        long milliseconds = time - TimeUnit.MINUTES.toMillis(minutes) - TimeUnit.SECONDS.toMillis(seconds);
        if (roundToTicks) milliseconds -= milliseconds % 50;
        return String.format("%02d:%02d.%03d", minutes, seconds, milliseconds);
    }

    public static @NotNull String formatPlayerPlaytime(long time) {
        var result = new StringBuilder();
        var days = time / 86400000;
        if (days > 0) {
            result.append(days).append("d ");
            time %= 86400000;
        }

        var hours = time / 3600000;
        if (hours > 0) {
            result.append(hours).append("h ");
            time %= 3600000;
        }

        var minutes = time / 60000;
        if (minutes > 0) {
            result.append(minutes).append("m ");
            time %= 60000;
        }

        if (result.isEmpty()) {
            // Less than 1m
            return "0m ";
        }

        return result.toString();
    }

    public static @NotNull String formatDuration(long time) {
        var result = new StringBuilder();
        var days = time / 86400000;
        if (days > 0) {
            result.append(days).append("d ");
            time %= 86400000;
        }

        var hours = time / 3600000;
        if (hours > 0) {
            result.append(hours).append("h ");
            time %= 3600000;
        }

        var minutes = time / 60000;
        if (minutes > 0) {
            result.append(minutes).append("m ");
            time %= 60000;
        }

        var seconds = time / 1000.0;
        if (seconds > 0) {
            result.append(String.format("%.2f", seconds)).append("s ");
        }

        if (result.isEmpty()) {
            // Less than 1s
            return "0s ";
        }

        return result.toString();
    }

    public static String formatTimeSince(@NotNull Instant time) {
        return formatDuration(Instant.now(), time);
    }

    public static String formatTimeUntil(@NotNull Instant time) {
        return formatDuration(time, Instant.now());
    }

    public static String formatDuration(@NotNull Instant now, @NotNull Instant start) {
        long seconds = now.getEpochSecond() - start.getEpochSecond();
        if (seconds < 60) return seconds + "s ";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m ";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h ";
        long days = hours / 24;
        return days + "d ";
    }

    private NumberUtil() {
    }
}
