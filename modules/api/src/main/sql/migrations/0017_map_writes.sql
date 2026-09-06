-- Apply by hand before deploying map writes (also to local compose Postgres).
-- Check duplicate published_id values, including deleted maps, before creating this index.
-- On production, run the index statements separately with CONCURRENTLY.
create unique index maps_published_id_unique on maps (published_id) where published_id is not null;
create index map_slots_map_idx on map_slots (map_id);
create index map_worlds_map_idx on map_worlds (map_id);
create index player_notifications_key_idx on player_notifications (player_id, type, key) where deleted_at is null;
