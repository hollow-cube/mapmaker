package net.hollowcube.mapmaker.hub.util;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;


public class HubTime {

    private static final ZoneOffset EST = ZoneOffset.ofHours(-5);
    private static final ZoneOffset ZONE_OFFSET = ZoneOffset.ofHours(-17);

    public static LocalDateTime now() {
        return LocalDateTime.now(ZONE_OFFSET);
    }

    public static class Christmas {

        public static boolean isActive() {
            return getDay() > 0;
        }

        public static int getDay() {
            LocalDateTime now = now();
            if (now.getMonthValue() == 1 && LocalDateTime.now(EST).getDayOfMonth() <= 10) {
                return 31 + now.getDayOfMonth();
            } else if (now.getMonthValue() == 12) {
                return now.getDayOfMonth();
            }
            return 0;
        }

        public static long getSecondsUntilDay(int day) {
            LocalDateTime now = now();
            if (now.getMonthValue() != 12) {
                return -1;
            } else {
                LocalDateTime dayDateTime = now.with(LocalTime.MIDNIGHT).withDayOfMonth(day);
                return now.until(dayDateTime, ChronoUnit.SECONDS);
            }
        }
    }
}
