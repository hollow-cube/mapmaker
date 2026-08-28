-- name: scheduleJob :exec
-- One row per recurring job, created once; a row that already exists keeps its own run_at.
insert into jobs (job, instance, run_at)
values ($job, $instance, $runAt)
on conflict (job, instance) do nothing;

-- name: enqueueJob :exec
-- Asks for a run, whenever there is room. An existing row is made due again — its backoff and its
-- failures forgotten, unparked, its data replaced if there is new data — and bumped, so that a run
-- already in flight does not delete it when it completes.
insert into jobs (job, instance, data)
values ($job, $instance, $data)
on conflict (job, instance) do update
    set run_at    = null,
        parked_at = null,
        attempts  = 0,
        data      = coalesce(excluded.data, jobs.data),
        version   = jobs.version + 1;

-- name: pickJobs :many
-- Takes up to $limit due rows of the named jobs for $who, in one statement. `skip locked` is what
-- keeps two replicas from taking the same row; the limit is the caller's free slots, never more,
-- so a replica cannot hold work it is not running. Timed rows first, oldest first. picked_at is the
-- wall clock rather than the transaction's now(), so that two picks are never the same pick.
with picked as (
    update jobs
        set picked_by = $who, picked_at = clock_timestamp(), heartbeat = now()
        where (job, instance) in (select job, instance
                                  from jobs
                                  where picked_by is null
                                    and parked_at is null
                                    and (run_at is null or run_at <= now())
                                    and job = any ($jobs)
                                  order by run_at nulls last, job, instance
                                      for update skip locked
                                  limit $limit)
        returning *)
select picked.*
from picked
order by run_at nulls last, job, instance;

-- name: heartbeatJobs :exec
-- Everything $who holds, in one statement.
update jobs
set heartbeat = now()
where picked_by = $who;

-- name: completeJob :exec
-- A recurring run succeeded; the row goes back to waiting for its next time.
update jobs
set picked_by    = null,
    picked_at    = null,
    heartbeat    = null,
    run_at       = $runAt,
    attempts     = 0,
    last_error   = null,
    last_success = now()
where job = $job
  and instance = $instance
  and picked_by = $who
  and picked_at = $pickedAt;

-- name: finishJob :exec
-- A one-shot run succeeded. Deletes nothing if the row was re-enqueued while it ran — the version
-- moved — in which case it is due again and releaseJob lets it go.
delete
from jobs
where job = $job
  and instance = $instance
  and picked_by = $who
  and picked_at = $pickedAt
  and version = $version;

-- name: releaseJob :exec
update jobs
set picked_by    = null,
    picked_at    = null,
    heartbeat    = null,
    attempts     = 0,
    last_error   = null,
    last_success = now()
where job = $job
  and instance = $instance
  and picked_by = $who
  and picked_at = $pickedAt;

-- name: failJob :exec
-- The run failed; the row is due again at $runAt, which for a one-shot is its backoff.
update jobs
set picked_by  = null,
    picked_at  = null,
    heartbeat  = null,
    run_at     = $runAt,
    attempts   = attempts + 1,
    last_error = $error
where job = $job
  and instance = $instance
  and picked_by = $who
  and picked_at = $pickedAt;

-- name: parkJob :exec
update jobs
set picked_by  = null,
    picked_at  = null,
    heartbeat  = null,
    parked_at  = now(),
    attempts   = attempts + 1,
    last_error = $error
where job = $job
  and instance = $instance
  and picked_by = $who
  and picked_at = $pickedAt;

-- name: yieldJob :exec
-- The process is stopping mid-run: hand the row straight back, due now and ahead of the queue,
-- without it counting as a failure — a deploy is not the job's fault.
update jobs
set picked_by = null,
    picked_at = null,
    heartbeat = null,
    run_at    = now()
where job = $job
  and instance = $instance
  and picked_by = $who
  and picked_at = $pickedAt;

-- name: reviveDeadJobs :many
-- Rows whose holder stopped heartbeating are put back as due now. The run it was in counts as an
-- attempt, so a job that keeps killing its worker still parks eventually.
update jobs
set picked_by  = null,
    picked_at  = null,
    heartbeat  = null,
    run_at     = now(),
    attempts   = attempts + 1,
    last_error = 'lost by ' || picked_by
where picked_by is not null
  and heartbeat < now() - $deadSeconds::int * interval '1 second'
returning jobs.*;

-- name: listJobs :many
select jobs.*
from jobs
order by job, instance;
