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
