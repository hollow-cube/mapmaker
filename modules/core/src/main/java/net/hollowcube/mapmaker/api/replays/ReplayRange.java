package net.hollowcube.mapmaker.api.replays;

/// An inclusive HTTP byte range.
public record ReplayRange(long start, long endInclusive) {

    public ReplayRange {
        if (start < 0)
            throw new IllegalArgumentException("range start must not be negative");
        if (endInclusive < start)
            throw new IllegalArgumentException("range end must not precede its start");
    }

    public static ReplayRange ofLength(long start, long length) {
        if (length <= 0)
            throw new IllegalArgumentException("range length must be positive");
        return new ReplayRange(start, Math.addExact(start, length - 1));
    }

    String headerValue() {
        return "bytes=" + start + "-" + endInclusive;
    }
}
