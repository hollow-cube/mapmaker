-- Ours, not Go's: apply by hand to the maps database before binding the format 5 backfill. Build
-- the index with `create index concurrently` in production.

-- The format 5 backfill's scan. Drop it with the format 4 reader.
create index if not exists replays_legacy_format_idx on replays (id)
    where substring(current_preamble from 5 for 2) = '\x0004'::bytea;
