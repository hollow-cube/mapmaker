-- Ours, not Go's: apply by hand to the maps database, with `drop index concurrently` in production.

-- Every replay is format 5 now, so the format 4 backfill's scan is gone.
drop index if exists replays_legacy_format_idx;
