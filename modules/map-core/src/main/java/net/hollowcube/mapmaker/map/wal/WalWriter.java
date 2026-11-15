package net.hollowcube.mapmaker.map.wal;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.terraform.apply.ChangeTracker;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.entity.EntityAccessor;
import net.kyori.adventure.nbt.BinaryTag;
import net.minestom.server.instance.Instance;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.Task;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

/// Writes (an over estimate of) world changes ~once per second.
///
/// If a batch fails we immediately do a full save to capture the failed changes, as well as another save
/// once we are able to resume sending (since there is a hole in the log there).
public final class WalWriter {
    private static final Logger logger = LoggerFactory.getLogger(WalWriter.class);

    private static final int FLUSH_INTERVAL_TICKS = 20;
    static final int SECTION_BUDGET = 512; // max captured per tick
    static final int BATCH_SECTIONS = 4096; // max held while a change is still being captured
    private static final int MAX_QUEUED = 2;
    private static final long CLOSE_TIMEOUT_MILLIS = 10_000;

    /// @param bytes an encoded [WalBatch]
    public record Batch(long firstSeq, long lastSeq, long firstAt, long lastAt, byte[] bytes) {}

    private final Instance instance;
    private final ChangeTracker tracker;
    private final NetworkBuffer.Type<EditBuffer> edits;
    private final Consumer<Batch> sink;
    private final Runnable onGap;

    // flush runs on the instance's tick thread and on whichever thread is saving the world
    private final ReentrantLock lock = new ReentrantLock();
    private final List<WalRecord> pending = new ArrayList<>();
    private int pendingSections;
    private long lastSeq;
    private volatile boolean backlog;
    private boolean closed;
    private CompletableFuture<Void> delivery = CompletableFuture.completedFuture(null);

    private final AtomicInteger queued = new AtomicInteger();
    // Only touched by delivery, which runs one batch at a time.
    private long dropThrough;
    private boolean gap;

    private @Nullable Task task;

    /// @param headSeq the last sequence already in the map's log
    /// @param onGap see the class doc; called off the tick thread
    public WalWriter(
        Instance instance,
        @Nullable EntityAccessor entities,
        NetworkBuffer.Type<EditBuffer> edits,
        long headSeq,
        Consumer<Batch> sink,
        Runnable onGap
    ) {
        this.instance = instance;
        this.tracker = ChangeTracker.attach(instance, entities);
        this.edits = edits;
        this.lastSeq = headSeq;
        this.sink = sink;
        this.onGap = onGap;
    }

    public void start() {
        task = instance.scheduler().submitTask(this::tick, ExecutionType.TICK_END);
    }

    private TaskSchedule tick() {
        flush(SECTION_BUDGET);
        // A flush the budget cut short carries on next tick, so a large change is not a second per batch.
        return backlog ? TaskSchedule.nextTick() : TaskSchedule.tick(FLUSH_INTERVAL_TICKS);
    }

    public void setWorldTag(int dataVersion, String key, @Nullable BinaryTag value) {
        lock.lock();
        try {
            if (closed) return;
            pending.add(new WalRecord.WorldTag(System.currentTimeMillis(), dataVersion, key, value));
        } finally {
            lock.unlock();
        }
    }

    /// Captures everything that has changed and queues it. Every record up to the returned sequence describes
    /// a change the instance already holds, so a world saved after this call contains them all.
    public long flush() {
        return flush(Integer.MAX_VALUE);
    }

    private long flush(int sectionBudget) {
        lock.lock();
        try {
            if (closed) return lastSeq;
            boolean budgeted = sectionBudget != Integer.MAX_VALUE;
            if (budgeted && queued.get() >= MAX_QUEUED) return lastSeq;

            EditBuffer changes = tracker.drain(sectionBudget);
            backlog = budgeted && !tracker.isEmpty();
            if (changes.sectionCount() != 0 || !changes.entities().isEmpty()) {
                pending.add(new WalRecord.Edit(System.currentTimeMillis(), changes));
                pendingSections += changes.sectionCount();
            }
            if (pending.isEmpty()) return lastSeq;
            // A change too large for one tick goes out as one batch once it is all captured, not one a tick.
            if (backlog && pendingSections < BATCH_SECTIONS) return lastSeq;

            var batch = new WalBatch(lastSeq + 1, pending);
            pending.clear();
            pendingSections = 0;
            lastSeq = batch.lastSeq();
            queued.incrementAndGet();
            delivery = delivery.handleAsync((_, _) -> {
                deliver(batch);
                return null;
            }, FutureUtil.VIRUTAL_EXECUTOR);
            return lastSeq;
        } finally {
            lock.unlock();
        }
    }

    private void deliver(WalBatch batch) {
        try {
            if (batch.lastSeq() <= dropThrough) return;

            var records = batch.records();
            sink.accept(new Batch(
                batch.firstSeq(), batch.lastSeq(),
                records.getFirst().time(), records.getLast().time(),
                batch.encode(edits)
            ));
            if (gap) {
                gap = false;
                notifyGap();
            }
        } catch (Throwable t) {
            lock.lock();
            try {
                dropThrough = lastSeq;
            } finally {
                lock.unlock();
            }
            if (gap) {
                logger.warn("dropped wal records {} through {} of {}: {}", batch.firstSeq(), dropThrough, instance.getUuid(), t.toString());
            } else {
                logger.error("dropped wal records {} through {} of {}", batch.firstSeq(), dropThrough, instance.getUuid(), t);
                gap = true;
                notifyGap();
            }
        } finally {
            queued.decrementAndGet();
        }
    }

    private void notifyGap() {
        try {
            onGap.run();
        } catch (Throwable t) {
            logger.error("wal gap callback of {} failed", instance.getUuid(), t);
        }
    }

    /// Stops capturing without sending what has changed since the last flush.
    public void stop() {
        close(false);
    }

    /// Sends what is left and waits, briefly, for it to be delivered.
    public void close() {
        close(true);
    }

    private void close(boolean flush) {
        if (task != null) task.cancel();
        CompletableFuture<Void> remaining;
        lock.lock();
        try {
            if (closed) return;
            if (flush) flush(Integer.MAX_VALUE);
            closed = true;
            remaining = delivery;
        } finally {
            lock.unlock();
        }
        tracker.detach();
        if (!flush) return;
        try {
            remaining.get(CLOSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            logger.warn("wal of {} was still being delivered at close", instance.getUuid(), e);
        }
    }
}
