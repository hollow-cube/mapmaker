-- One row per full world save. The object is `worlds/{hex sha256}` in the world store bucket and is
-- shared by every row, of any map, with that digest. A map's head is its row with the highest id.
create table map_world_versions
(
    map_id     uuid        not null,
    id         bigint generated always as identity,
    sha256     bytea       not null,
    length     bigint      not null,
    -- Every write-ahead-log record up to this one is already in the object.
    wal_seq    bigint      not null,
    -- The `map_worlds` row that saved it; null for a save from a server that does not claim one.
    world_id   text,
    created_at timestamptz not null default now(),

    primary key (map_id, id),
    constraint map_world_versions_sha256_is_sha256 check (octet_length(sha256) = 32)
);
alter table map_worlds add column if not exists type text;
create unique index if not exists map_worlds_edit_map_idx on map_worlds (map_id) where type = 'editing';
