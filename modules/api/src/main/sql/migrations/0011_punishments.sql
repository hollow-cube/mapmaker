create table if not exists punishments
(
    id             serial primary key,
    player_id      varchar(36) not null,
    executor_id    varchar(36) not null,
    type           varchar(4)  not null, -- "ban", "kick", "mute"
    created_at     timestamptz not null,
    ladder_id      varchar     default null,
    comment        varchar     not null,
    expires_at     timestamptz default null,
    revoked_by     varchar(36) default null,
    revoked_at     timestamptz default null,
    revoked_reason varchar     default null
);

create index if not exists punishments_active_idx on punishments (player_id, type) where revoked_by is null;
