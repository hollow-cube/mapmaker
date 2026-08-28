package net.hollowcube.apiserver.job;

import com.google.gson.Gson;
import net.hollowcube.apiserver.db.JobsQueries;
import org.jetbrains.annotations.Nullable;

import java.util.function.Function;

/// One kind of background work, as both the process that asks for it and the worker that does it
/// see it: its name in the `jobs` table, the record its `data` column holds, and how it is run.
///
/// A timed spec has a [#schedule] and one row, which the worker keeps moving forward. A queued
/// spec has a row per request, keyed by [#instance] of its data so that asking twice is one row,
/// deleted when it succeeds and retried with backoff until [#maxAttempts], after which it is
/// parked for a human. Every spec there is lives here, so that the producer's record and the
/// worker's are the same class.
///
/// @param instance what makes one request distinct from another of the same job; `-` for timed
public record JobSpec<D>(String name, Class<D> data, Function<D, String> instance, @Nullable Cron schedule, int maxAttempts) {
    private static final Gson GSON = new Gson();
    private static final int DEFAULT_MAX_ATTEMPTS = 5;
    public static final String TIMED_INSTANCE = "-";

    public static final JobSpec<Void> PLAYER_COUNT = timed("player-count", "*/5 * * * *");

    public static JobSpec<Void> timed(String name, String cron) {
        return new JobSpec<>(name, Void.class, ignored -> TIMED_INSTANCE, Cron.parse(cron), DEFAULT_MAX_ATTEMPTS);
    }

    public static <D> JobSpec<D> queued(String name, Class<D> data, Function<D, String> instance) {
        return new JobSpec<>(name, data, instance, null, DEFAULT_MAX_ATTEMPTS);
    }

    public JobSpec<D> attempts(int maxAttempts) {
        return new JobSpec<>(name, data, instance, schedule, maxAttempts);
    }

    /// Asks for a run of a queued job. Inside a transaction this is `tx.jobs`, so the request
    /// commits with whatever caused it or not at all.
    public void enqueue(JobsQueries jobs, D data) {
        if (schedule != null) throw new IllegalArgumentException(name + " is timed; it has its row already");
        jobs.enqueueJob(name, instance.apply(data), GSON.toJson(data));
    }

    /// What a row's `data` column holds, as the record; null data is a null record.
    public @Nullable D decode(@Nullable String json) {
        return json == null ? null : GSON.fromJson(json, data);
    }
}
