package net.hollowcube.common.lang;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;

import java.text.DateFormat;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;

public class TimeComponent {

    public static Component of(Instant instant) {
        return of(instant, false);
    }

    public static Component of(Instant instant, boolean future) {
        long diff = instant.until(Instant.now(), ChronoUnit.MILLIS);
        var suffix = future ? ".future" : ".past";

        if (diff <= 0) {
            return Component.translatable("time.now");
        } else {
            long seconds = diff / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            long days = hours / 24;
            long months = days / 30;
            long years = months / 12;


            var builder = Component.text();

            if (years > 1) {
                builder.append(Component.translatable("time.years" + suffix, Component.text(years)));
            } else if (years == 1) {
                builder.append(Component.translatable("time.year" + suffix));
            } else if (months > 1) {
                builder.append(Component.translatable("time.months" + suffix, Component.text(months)));
            } else if (months == 1) {
                builder.append(Component.translatable("time.month" + suffix));
            } else if (days > 1) {
                builder.append(Component.translatable("time.days" + suffix, Component.text(days)));
            } else if (days == 1) {
                builder.append(Component.translatable("time.day" + suffix));
            } else if (hours > 1) {
                builder.append(Component.translatable("time.hours" + suffix, Component.text(hours)));
            } else if (hours == 1) {
                builder.append(Component.translatable("time.hour" + suffix));
            } else if (minutes > 1) {
                builder.append(Component.translatable("time.minutes" + suffix, Component.text(minutes)));
            } else if (minutes == 1) {
                builder.append(Component.translatable("time.minute" + suffix));
            } else if (seconds > 1) {
                builder.append(Component.translatable("time.seconds" + suffix, Component.text(seconds)));
            } else {
                builder.append(Component.translatable("time.second" + suffix));
            }

            var formatter = DateFormat.getDateTimeInstance(DateFormat.FULL, DateFormat.MEDIUM, Locale.CANADA);

            return builder.hoverEvent(HoverEvent.showText(
                    Component.text(formatter.format(Date.from(instant)))
            )).build();
        }
    }
}
