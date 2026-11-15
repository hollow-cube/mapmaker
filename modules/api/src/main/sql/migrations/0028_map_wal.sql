create table map_wal
(
    map_id        uuid        not null,
    first_seq     bigint      not null,
    last_seq      bigint      not null,
    world_id      text        not null,
    first_at      timestamptz not null,
    last_at       timestamptz not null,
    length        int         not null,
    data          bytea, -- inline for small entries
    object_sha256 bytea, -- wal/{sha256} in r2 for big entries

    primary key (map_id, first_seq),
    constraint map_wal_data_or_object check ((data is null) != (object_sha256 is null)),
    constraint map_wal_object_is_sha256 check (object_sha256 is null or octet_length(object_sha256) = 32)
);
-- we compress already so pglz on it after is a waste
alter table map_wal alter column data set storage external;
