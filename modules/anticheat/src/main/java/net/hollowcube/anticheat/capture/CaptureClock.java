package net.hollowcube.anticheat.capture;

import java.time.Instant;

/// Where the capture engine gets its time.
///
/// Frame timestamps come from the tap and have to be from the same source as [#nanoTime()], which
/// in production is [System#nanoTime()] on both sides. Tests inject a clock they step by hand, so a
/// window that takes ninety seconds to fill takes no time to check.
@FunctionalInterface
public interface CaptureClock {

    CaptureClock SYSTEM = System::nanoTime;

    long nanoTime();

    /// Wall clock, which only the header's `startedAt`/`endedAt` are allowed to care about.
    default Instant instant() {
        return Instant.now();
    }
}
