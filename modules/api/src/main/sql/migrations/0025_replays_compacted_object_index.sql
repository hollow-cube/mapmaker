-- Ours, not Go's: apply by hand to the maps database before running sweep-replay-objects. Build the
-- index with `create index concurrently` in production.

-- The orphan sweep's lookup from an object key back to the replay that points at it.
create index if not exists replays_compacted_object_idx on replays (compacted_object)
    where compacted_object is not null;
