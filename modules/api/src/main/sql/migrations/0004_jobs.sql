-- The api-worker's schedule and queue: one row per (job, instance). A recurring job keeps one row
-- and moves run_at forward after every run; a one-shot row is deleted when it succeeds. Nothing
-- else needs to know which is which.
--
-- run_at is "not before": a row with one is timed, and due once it has passed. A row without one
-- is due whenever, and is taken only after everything timed that is due, so a queue of ten
-- thousand of them does not put a five-minute job behind them. A failed one-shot gets a run_at
-- for its backoff, which is what puts a retry ahead of fresh work.
--
-- parked_at is set when a row has failed too many times and is left for a human, who unparks it
-- by clearing it. picked_by null is not running; picked_at and heartbeat are only meaningful while
-- it is. picked_at is what a run hands back to prove the row is still its own: a row revived and
-- picked again — even by the same replica — has a new one, and the old run's outcome no longer
-- applies to it.
create table if not exists jobs
(
    job          text        not null,
    instance     text        not null,
    data         jsonb,
    run_at       timestamptz,
    picked_by    text,
    picked_at    timestamptz,
    heartbeat    timestamptz,
    attempts     int         not null default 0,
    last_error   text,
    last_success timestamptz,
    parked_at    timestamptz,
    version      bigint      not null default 0,
    primary key (job, instance)
);

create index if not exists jobs_due_idx on jobs (run_at nulls last, job, instance) where picked_by is null and parked_at is null;
create index if not exists jobs_running_idx on jobs (heartbeat) where picked_by is not null;
