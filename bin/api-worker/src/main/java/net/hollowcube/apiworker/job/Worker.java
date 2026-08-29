package net.hollowcube.apiworker.job;

import net.hollowcube.apiserver.db.ApiDatabase;
import net.hollowcube.apiserver.db.Jobs;
import net.hollowcube.apiserver.db.JobsQueries;
import net.hollowcube.apiserver.job.Cron;
import net.hollowcube.apiserver.job.JobSpec;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/// Runs the rows of the `jobs` table that are due, and keeps them honest while they run.
///
/// The table is the whole of the coordination: a row is picked with `for update skip locked`, so
/// replicas never take the same one and there is no leader to elect. A replica only ever picks as
/// many rows as it has free slots, so it cannot sit on work another replica could be doing, and
/// timed rows are picked before the queue, so a timed job waits for one slot at most. While a run
/// is in flight its row is heartbeated; a row whose heartbeat stops is revived for someone else.
/// Every outcome a run reports carries the row's `picked_at`, so a run whose row was revived and
/// picked again — by anyone, this replica included — no longer speaks for it.
///
/// Three loops, each on its own thread: pick due rows, heartbeat the running ones, revive the dead
/// ones. Runs go to [#executor], a virtual thread each by default.
public final class Worker implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(Worker.class);

    /// Rows that are picked but not yet running would look like this to the reaper, which is why
    /// picking is bounded by free slots: everything picked is running within the same poll.
    static final Duration HEARTBEAT = Duration.ofSeconds(10);
    static final int MISSED_HEARTBEATS_BEFORE_DEAD = 4;
    private static final Duration POLL = Duration.ofSeconds(5);
    private static final Duration MAX_BACKOFF = Duration.ofHours(1);

    private final ApiDatabase db;
    private final String who;
    private final Executor executor;
    private final Semaphore slots;
    private final Map<String, Bound<?>> jobs = new LinkedHashMap<>();
    private final List<String> names = new ArrayList<>();
    private final ConcurrentHashMap<Key, Running> running = new ConcurrentHashMap<>();
    /// Released by anything that frees a slot or adds a row, so the picker does not wait out a poll.
    private final Semaphore wakeups = new Semaphore(0);
    private final List<Thread> loops = new ArrayList<>();
    private volatile boolean scheduled;
    private volatile boolean stopped;

    /// @param who   what a picked row records as its holder; the pod name in the cluster
    /// @param slots how many runs this replica has going at once
    public Worker(ApiDatabase db, String who, int slots) {
        this(db, who, slots, task -> Thread.ofVirtual().start(task));
    }

    /// @param executor where runs go; a test passes `Runnable::run` to make everything synchronous
    public Worker(ApiDatabase db, String who, int slots, Executor executor) {
        this.db = db;
        this.who = who;
        this.executor = executor;
        this.slots = new Semaphore(slots);
    }

    /// Binds the code for a spec. Rows of specs this worker has no runner for are left for one
    /// that does.
    public <D> void handle(JobSpec<D> spec, JobRunner<D> runner) {
        if (!loops.isEmpty()) throw new IllegalStateException("jobs are bound before start");
        if (jobs.putIfAbsent(spec.name(), new Bound<>(spec, runner)) != null)
            throw new IllegalArgumentException("two runners for " + spec.name());
        names.add(spec.name());
    }

    /// Starts the loops. Nothing here touches the database: a database that is unreachable at
    /// startup is not a reason to fail, and the first poll that gets through creates the recurring
    /// rows.
    public void start() {
        // Platform threads, not virtual: they are what keeps the process alive, since it listens
        // on nothing. Runs themselves are virtual.
        loops.add(Thread.ofPlatform().name("jobs-pick").start(() -> loop(POLL, this::pollOnce, true)));
        loops.add(Thread.ofPlatform().name("jobs-heartbeat").start(() -> loop(HEARTBEAT, this::heartbeat, false)));
        loops.add(Thread.ofPlatform().name("jobs-reap").start(() -> loop(HEARTBEAT.multipliedBy(2), this::revive, false)));
        logger.info("running {} as {} ({} slots)", jobs.keySet(), who, slots.availablePermits());
    }

    /// Creates the recurring rows that do not exist yet. An existing row keeps the time it has.
    public void scheduleRecurring() {
        for (var bound : jobs.values()) {
            var schedule = bound.spec.schedule();
            if (schedule != null) db.jobs.scheduleJob(bound.spec.name(), JobSpec.TIMED_INSTANCE, schedule.next(Instant.now()));
        }
        scheduled = true;
    }

    /// Picks what is due and this replica has room for, and starts it. Returns how many.
    public int pollOnce() {
        if (!scheduled) scheduleRecurring();
        var free = slots.availablePermits();
        if (free == 0 || names.isEmpty()) return 0;
        int started = 0;
        for (var row : db.jobs.pickJobs(who, names, free)) {
            // Picked while close() was already running: it would neither be interrupted nor
            // waited for, so it goes straight back.
            if (stopped) {
                handBack(row);
                continue;
            }
            slots.acquireUninterruptibly();
            try {
                start(row);
            } catch (RuntimeException e) {
                slots.release();
                handBack(row);
                throw e;
            }
            started++;
        }
        return started;
    }

    /// Asks the picker to look now rather than at its next poll, after a row was added in-process.
    public void wake() {
        wakeups.release();
    }

    /// Stops picking, interrupts the runs still in their job body and waits up to `grace` for
    /// everything to hand its row back. A run that is still going after that is left to the
    /// reaper on another replica.
    @Override
    public void close() {
        close(Duration.ofSeconds(20));
    }

    public void close(Duration grace) {
        stopped = true;
        for (var loop : loops) loop.interrupt();
        var deadline = System.nanoTime() + grace.toNanos();
        while (!running.isEmpty() && System.nanoTime() < deadline) {
            // Only a run still inside the job is interrupted: one that is reporting its outcome
            // is about to let go of the row, and interrupting its database call would not.
            for (var run : running.values()) {
                var thread = run.thread;
                if (thread != null && run.inBody) thread.interrupt();
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
        for (var key : running.keySet()) logger.warn("{}/{} is still running after {}, abandoning it", key.job, key.instance, grace);
    }

    private void loop(Duration every, Runnable body, boolean wakeable) {
        while (!stopped) {
            try {
                body.run();
            } catch (Exception e) {
                // Exception, not RuntimeException: the generated queries rethrow SQLException as is.
                logger.warn("{} failed: {}", Thread.currentThread().getName(), e.getMessage());
            } catch (Error e) {
                // A loop that has died leaves a process that looks alive and does nothing; better to
                // be restarted.
                logger.error("{} died, halting", Thread.currentThread().getName(), e);
                Runtime.getRuntime().halt(1);
            }
            try {
                if (wakeable) {
                    wakeups.tryAcquire(every.toMillis(), TimeUnit.MILLISECONDS);
                    wakeups.drainPermits();
                } else {
                    Thread.sleep(every);
                }
            } catch (InterruptedException e) {
                return;
            }
        }
    }

    private void start(Jobs row) {
        var key = new Key(row.job(), row.instance());
        var run = new Running();
        // Registered before the run exists, so close() cannot miss one that is about to start.
        running.put(key, run);
        try {
            executor.execute(() -> {
                run.thread = Thread.currentThread();
                try {
                    execute(row, run);
                } finally {
                    running.remove(key, run);
                    slots.release();
                    wake();
                }
            });
        } catch (RuntimeException e) {
            running.remove(key, run);
            throw e;
        }
    }

    private void execute(Jobs row, Running running) {
        var bound = jobs.get(row.job());
        var attempt = row.attempts() + 1;
        var start = System.nanoTime();
        running.inBody = true;
        try {
            bound.run(row.data());
        } catch (Undecodable e) {
            // Data that does not decode will not decode next time either.
            running.inBody = false;
            logger.error("{}/{}: {}", row.job(), row.instance(), e.getMessage());
            report(row, () -> db.jobs.parkJob(e.getMessage(), row.job(), row.instance(), who, row.pickedAt()));
            return;
        } catch (Exception e) {
            running.inBody = false;
            // Only close() interrupts a run, and a deploy is not the job's fault. The flag is
            // cleared either way: a pool will not hand a connection to an interrupted thread, and
            // the row has to be reported on whatever happened.
            if (Thread.interrupted() || e instanceof InterruptedException) {
                logger.info("{}/{} interrupted, handing it back", row.job(), row.instance());
                handBack(row);
                return;
            }
            logger.error("{}/{} failed on attempt {}", row.job(), row.instance(), attempt, e);
            report(row, () -> failed(bound.spec, row, attempt, e.toString()));
            return;
        } catch (Error e) {
            // Out of memory, a class that would not load: the row is reported like any failure,
            // so a map that keeps doing this parks, and the error goes on to whatever it was
            // going to do to the process.
            running.inBody = false;
            Thread.interrupted();
            logger.error("{}/{} died on attempt {}", row.job(), row.instance(), attempt, e);
            report(row, () -> failed(bound.spec, row, attempt, e.toString()));
            throw e;
        }
        running.inBody = false;
        Thread.interrupted();
        logger.info("{}/{} done in {}ms", row.job(), row.instance(), (System.nanoTime() - start) / 1_000_000);
        report(row, () -> succeeded(bound.spec, row));
    }

    /// Reporting is best effort: a row whose outcome could not be written stays picked until the
    /// reaper revives it, which costs a repeat rather than a crash of the thread.
    private void report(Jobs row, Runnable outcome) {
        try {
            outcome.run();
        } catch (Exception e) {
            logger.warn("{}/{}: could not report its outcome, the reaper will have it ({})", row.job(), row.instance(), e.getMessage());
        }
    }

    private void handBack(Jobs row) {
        report(row, () -> db.jobs.yieldJob(row.job(), row.instance(), who, row.pickedAt()));
    }

    private void succeeded(JobSpec<?> spec, Jobs row) {
        var schedule = spec.schedule();
        if (schedule != null) {
            db.jobs.completeJob(next(schedule, row), row.job(), row.instance(), who, row.pickedAt());
            return;
        }
        // Zero rows means it was re-enqueued while it ran and is due again; let go of it as is.
        if (db.jobs.finishJob(row.job(), row.instance(), who, row.pickedAt(), row.version()) == 0)
            db.jobs.releaseJob(row.job(), row.instance(), who, row.pickedAt());
    }

    private void failed(JobSpec<?> spec, Jobs row, int attempt, String error) {
        var schedule = spec.schedule();
        if (schedule != null) {
            db.jobs.failJob(new JobsQueries.FailJobParams(next(schedule, row), error, row.job(), row.instance(), who, row.pickedAt()));
        } else if (attempt >= spec.maxAttempts()) {
            logger.warn("{}/{} parked after {} attempts", row.job(), row.instance(), attempt);
            db.jobs.parkJob(error, row.job(), row.instance(), who, row.pickedAt());
        } else {
            db.jobs.failJob(new JobsQueries.FailJobParams(Instant.now().plus(backoff(attempt)), error, row.job(), row.instance(), who, row.pickedAt()));
        }
    }

    /// The boundary after the one that fired, whatever this JVM's clock thinks: the row was due by
    /// the database's clock, and a JVM a little behind it would otherwise hand the same boundary
    /// straight back and run it twice.
    private static Instant next(Cron schedule, Jobs row) {
        var now = Instant.now();
        var fired = row.runAt();
        return schedule.next(fired != null && fired.isAfter(now) ? fired : now);
    }

    /// 1s, 16s, 81s, 4m, 10m, ... — attempt to the fourth, the curve graphile-worker settled on.
    static Duration backoff(int attempt) {
        var seconds = Math.min(MAX_BACKOFF.toSeconds(), (long) Math.pow(attempt, 4));
        return Duration.ofSeconds(seconds);
    }

    private void heartbeat() {
        if (!running.isEmpty()) db.jobs.heartbeatJobs(who);
    }

    private void revive() {
        var deadSeconds = (int) HEARTBEAT.multipliedBy(MISSED_HEARTBEATS_BEFORE_DEAD).toSeconds();
        for (var row : db.jobs.reviveDeadJobs(deadSeconds))
            logger.warn("{}/{} was lost by {}, due again", row.job(), row.instance(), row.lastError());
    }

    private record Key(String job, String instance) {
    }

    /// A spec and its runner, which is where the row's json becomes the runner's record.
    private record Bound<D>(JobSpec<D> spec, JobRunner<D> runner) {
        void run(@Nullable String json) throws Exception {
            D data;
            try {
                data = spec.decode(json);
            } catch (RuntimeException e) {
                throw new Undecodable("data is not a " + spec.data().getSimpleName() + ": " + e.getMessage());
            }
            // A row inserted by hand without its data will not grow any next time either.
            if (data == null && spec.data() != Void.class)
                throw new Undecodable("data is missing; " + spec.name() + " needs a " + spec.data().getSimpleName());
            runner.run(data);
        }
    }

    private static final class Undecodable extends Exception {
        Undecodable(String message) {
            super(message);
        }
    }

    /// One run's thread and where it is: in the job, or reporting on it.
    private static final class Running {
        volatile @Nullable Thread thread;
        volatile boolean inBody;
    }
}
