package net.hollowcube.terraform.util;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.time.Instant;

public final class Format {

    public static String formatBytes(long size) {
        if (size < 0) {
            throw new IllegalArgumentException("Invalid file size: " + size);
        }
        if (size < 1024) return size + "B";
        int unitIdx = (63 - Long.numberOfLeadingZeros(size)) / 10;
        return formatSize(size, 1L << (unitIdx * 10), " KMGTPE".charAt(unitIdx) + "iB");
    }

    private static DecimalFormat DEC_FORMAT = new DecimalFormat("#.##");

    private static String formatSize(long size, long divider, String unitName) {
        return DEC_FORMAT.format((double) size / divider) + " " + unitName;
    }

    public static String formatTimeSince(@NotNull Instant time) {
        return formatDuration(Instant.now(), time);
    }

    public static String formatTimeUntil(@NotNull Instant time) {
        return formatDuration(time, Instant.now());
    }

    public static String formatDuration(@NotNull Instant now, @NotNull Instant start) {
        long seconds = now.getEpochSecond() - start.getEpochSecond();
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m";
        long hours = minutes / 60;
        if (hours < 24) return hours + "h";
        long days = hours / 24;
        return days + "d";
    }

}
