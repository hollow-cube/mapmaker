package net.hollowcube.mapmaker.hub.feature.motw;

import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

public class CountdownTimer implements Runnable {
    private final Instance instance;

    private int[] digits = new int[]{-1, -1, -1, -1, -1};

    public CountdownTimer(@NotNull Instance instance) {
        this.instance = instance;

        // Run once immediately to preconfigure the timer
        run();
    }

    @Override
    public void run() {
        var remaining = (int) timeToNextWeek();

        var days = remaining / 60 / 24;
        remaining -= days * 60 * 24;
        var hours = remaining / 60;
        remaining -= hours * 60;
        var minutes = remaining;
        var newDigits = new int[]{days, hours / 10, hours % 10, minutes / 10, minutes % 10};

        // Diff the digits and change the relevant schematics
        for (int i = 0; i < 5; i++) {
            if (digits[i] == newDigits[i]) continue;
            digits[i] = newDigits[i];

            // Update the schematic
            CountdownUtil.applySchematic(instance, i, digits[i]);
        }
    }

    public long timeToNextMinute() {
        return 60 - (System.currentTimeMillis() / 1000 % 60);
    }

    public long timeToNextWeek() {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/New_York"));
        LocalDateTime nextSundayMidnight = now.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).toLocalDate().atStartOfDay();

        return ChronoUnit.MINUTES.between(now, nextSundayMidnight);
    }
}
