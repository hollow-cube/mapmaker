package net.hollowcube.apiserver.job;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.BitSet;

/// A five-field cron expression — `minute hour day-of-month month day-of-week` — evaluated in UTC.
///
/// Each field takes `*`, a number, a range `a-b`, a list `a,b,c` and a step `*/n` or `a-b/n`.
/// Days of the week are `0-7` with both 0 and 7 as Sunday. As in cron, when both day fields are
/// restricted a day matches if either does. That is every schedule anything here has wanted —
/// `*/5 * * * *`, `0 4 * * *` — and nothing below a minute, which nothing here wants.
public record Cron(String expression, BitSet minutes, BitSet hours, BitSet days, BitSet months, BitSet weekdays,
                   boolean anyDay, boolean anyWeekday) {

    /// Far enough that an expression with no next time — `0 0 31 2 *` — is caught, and no
    /// further, since each pass moves at least a minute and usually a field.
    private static final int MAX_STEPS = 10_000;

    public static Cron parse(String expression) {
        var fields = expression.strip().split("\\s+");
        if (fields.length != 5)
            throw new IllegalArgumentException("a cron expression has five fields, not " + fields.length + ": '" + expression + "'");
        var cron = new Cron(expression,
            field(fields[0], 0, 59), field(fields[1], 0, 23), field(fields[2], 1, 31), field(fields[3], 1, 12),
            weekdays(field(fields[4], 0, 7)),
            fields[2].equals("*"), fields[4].equals("*"));
        cron.next(Instant.EPOCH); // never matches → says so now, not at the first boundary
        return cron;
    }

    /// The first matching minute strictly after `now`.
    ///
    /// Checks the largest field first and, whenever one does not match, jumps to the next value
    /// it could and resets everything smaller, so a pass either matches or moves a whole field:
    /// a handful of passes for any expression, rather than a walk through every minute.
    public Instant next(Instant now) {
        var t = now.atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.MINUTES).plusMinutes(1);
        for (int step = 0; step < MAX_STEPS; step++) {
            if (!months.get(t.getMonthValue())) {
                t = t.plusMonths(1).withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
            } else if (!dayMatches(t)) {
                t = t.plusDays(1).truncatedTo(ChronoUnit.DAYS);
            } else if (!hours.get(t.getHour())) {
                int hour = hours.nextSetBit(t.getHour());
                t = hour < 0 ? t.plusDays(1).truncatedTo(ChronoUnit.DAYS) : t.withHour(hour).truncatedTo(ChronoUnit.HOURS);
            } else if (!minutes.get(t.getMinute())) {
                int minute = minutes.nextSetBit(t.getMinute());
                t = minute < 0 ? t.plusHours(1).truncatedTo(ChronoUnit.HOURS) : t.withMinute(minute);
            } else {
                return t.toInstant();
            }
        }
        throw new IllegalArgumentException("'" + expression + "' never matches");
    }

    private boolean dayMatches(ZonedDateTime t) {
        boolean day = days.get(t.getDayOfMonth());
        boolean weekday = weekdays.get(t.getDayOfWeek().getValue() % 7);
        if (anyDay) return weekday;
        if (anyWeekday) return day;
        return day || weekday;
    }

    private static BitSet field(String text, int min, int max) {
        var bits = new BitSet(max + 1);
        for (var part : text.split(",")) {
            var step = 1;
            var slash = part.indexOf('/');
            if (slash >= 0) {
                step = number(part.substring(slash + 1), 1, max, text);
                part = part.substring(0, slash);
            }
            int from, to;
            if (part.equals("*")) {
                from = min;
                to = max;
            } else if (part.contains("-")) {
                var dash = part.indexOf('-');
                from = number(part.substring(0, dash), min, max, text);
                to = number(part.substring(dash + 1), min, max, text);
                if (to < from) throw new IllegalArgumentException("cron range runs backwards: '" + text + "'");
            } else {
                from = number(part, min, max, text);
                to = slash >= 0 ? max : from;
            }
            for (int value = from; value <= to; value += step) bits.set(value);
        }
        return bits;
    }

    private static int number(String text, int min, int max, String field) {
        int value;
        try {
            value = Integer.parseInt(text);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("not a cron field: '" + field + "'");
        }
        if (value < min || value > max)
            throw new IllegalArgumentException("cron field '" + field + "' is outside " + min + "-" + max);
        return value;
    }

    /// 7 is Sunday too.
    private static BitSet weekdays(BitSet bits) {
        if (bits.get(7)) {
            bits.set(0);
            bits.clear(7);
        }
        return bits;
    }

    @Override
    public String toString() {
        return expression;
    }
}
