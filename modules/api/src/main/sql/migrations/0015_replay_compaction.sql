-- Ours, not Go's: apply by hand, to the maps database. Additive and safe to apply while Go serves.
--
-- `replays` is 1.4M rows in production, so build the indexes with `create index concurrently` there:
-- a plain `create index` blocks every commit for as long as it runs. It is not written that way
-- because `concurrently` cannot run in a transaction, and this file is also what sql-gen describes
-- against.

-- Why a finished recording will never be appended to again. The api cannot tell a completed run
-- from the hard reset that superseded it without parsing the replay, so the recorder says which;
-- ~95% are resets. Null on every row Go wrote.
alter table replays
    add column if not exists outcome text;
alter table replays
    drop constraint if exists replay_outcome_valid;
alter table replays
    add constraint replay_outcome_valid check (outcome is null or outcome in ('finished', 'reset'));

-- The reconciler's scan. Partial, so it costs a page once the backlog is worked through.
create index if not exists replays_uncompacted_idx on replays (updated_at)
    where state = 'finished' and representation = 'segmented';

-- The source-segment sweeper's scan.
create index if not exists replays_compacted_idx on replays (updated_at)
    where representation = 'compacted';

-- Idempotency expiry. Nothing has ever swept this, at ~88k rows a day.
create index if not exists replay_idempotency_created_idx on replay_idempotency (created_at);
