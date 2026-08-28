package net.hollowcube.apiworker.job;

import net.hollowcube.apiserver.job.JobSpec;
import org.jetbrains.annotations.Nullable;

/// What the worker does for one [JobSpec]: the code behind the row.
///
/// Runs are at-least-once. A replica that dies mid-run has its row revived for another, so a run
/// has to be safe to repeat. It can take as long as it likes: the [Worker] heartbeats the row for
/// it. A run that is interrupted is the process stopping, and should let the interrupt out.
///
/// @param <D> the spec's data; a timed job's is [Void] and always null
public interface JobRunner<D> {

    void run(@Nullable D data) throws Exception;
}
