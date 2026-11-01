package net.hollowcube.terraform.apply;

import net.minestom.server.MinecraftServer;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.SchedulerManager;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;

/// Shared timer for [ApplyQueue].
///
/// Apply occurs in the instance tick which happens on the scheduler manager thread (so it gets throttled by that
/// thread in practice but its fine). Does not have any attempt at a fairness mechanism.
final class TickDeadline {
    private static @Nullable SchedulerManager installedOn;
    private static long tickStart;
    private static boolean tickStarted;
    private static long lastQueueEnd;
    private static long afterQueuesNanos;

    /// The deadline for queues this tick, to keep the tick under `targetNanos`. May already have passed.
    static long next(long targetNanos) {
        ensureInstalled();
        // Not recorded this tick when the tracker was installed mid-tick
        long start = tickStarted ? tickStart : System.nanoTime();
        return start + targetNanos - afterQueuesNanos;
    }

    /// Called when a queue finishes running for this tick.
    static void queueFinished() {
        lastQueueEnd = System.nanoTime();
    }

    private static TaskSchedule startTick() {
        tickStart = System.nanoTime();
        tickStarted = true;
        return TaskSchedule.nextTick();
    }

    private static TaskSchedule endTick() {
        // Only measure when a queue ran this tick, otherwise keep the last measurement
        if (tickStarted && lastQueueEnd - tickStart > 0)
            afterQueuesNanos = System.nanoTime() - lastQueueEnd;
        tickStarted = false;
        return TaskSchedule.nextTick();
    }

    private static void ensureInstalled() {
        var scheduler = MinecraftServer.getSchedulerManager();
        if (scheduler == installedOn) return;

        installedOn = scheduler; // A new server process, reset what the last one measured
        tickStarted = false;
        afterQueuesNanos = 0;

        scheduler.submitTask(TickDeadline::startTick, ExecutionType.TICK_START);
        scheduler.submitTask(TickDeadline::endTick, ExecutionType.TICK_END);
    }

    private TickDeadline() {}
}
