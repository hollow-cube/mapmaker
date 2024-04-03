package net.hollowcube.mapmaker.hub.feature.motw;

import net.minestom.server.instance.Instance;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.NotNull;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.function.Supplier;

public class CountdownTimer implements Supplier<TaskSchedule> {
    private final Instance instance;

    private final int[] digits = new int[]{-1, -1, -1, -1, -1};

    public CountdownTimer(@NotNull Instance instance) {
        this.instance = instance;
    }

    @Override
    public TaskSchedule get() {
        var remaining = (int) timeToNextWeek();
        if (remaining < 0) {
            setDigits(new int[]{0, 0, 0, 0, 0});
            return TaskSchedule.stop();
        }

        var days = remaining / 60 / 24;
        remaining -= days * 60 * 24;
        var hours = remaining / 60;
        remaining -= hours * 60;
        var minutes = remaining;
        var newDigits = new int[]{days % 9, hours / 10, hours % 10, minutes / 10, minutes % 10};
        setDigits(newDigits);

        return TaskSchedule.seconds(timeToNextMinute());
    }

    public long timeToNextMinute() {
        return 60 - (System.currentTimeMillis() / 1000 % 60);
    }

    public long timeToNextWeek() {
//        LocalDateTime now = LocalDateTime.now(ZoneId.of("America/New_York"));
//        LocalDateTime nextSundayMidnight = now.with(TemporalAdjusters.next(DayOfWeek.SUNDAY)).toLocalDate().atStartOfDay();
//        return ChronoUnit.MINUTES.between(now, nextSundayMidnight);
        var now = LocalDateTime.now(ZoneId.of("America/New_York"));
        var startOfOpenBetaTime = LocalDateTime.parse("2024-04-05T10:00:00-04:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        var endOfOpenBetaTime = LocalDateTime.parse("2024-04-07T23:59:00-04:00", DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        return ChronoUnit.MINUTES.between(now, now.isAfter(startOfOpenBetaTime) ? endOfOpenBetaTime : startOfOpenBetaTime);
    }

    public void setDigits(int[] newDigits) {
        // Diff the digits and change the relevant schematics
        for (int i = 0; i < 5; i++) {
            if (digits[i] == newDigits[i]) continue;
            digits[i] = newDigits[i];

            // Update the schematic
            CountdownUtil.applySchematic(instance, i, digits[i]);
        }
    }
}
