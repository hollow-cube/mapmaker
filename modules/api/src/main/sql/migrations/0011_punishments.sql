-- The Go api-server's punishments, as its `internal/playerdb` migration 000001 leaves the table,
-- in the players database. Go owns the schema; only the index below is ours.
--
-- Chat reads one question of it — is this player muted — on every message.
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

-- Ours, not Go's: apply by hand, to the players database.
--
-- The table has never had one, and the mute check now runs on every chat message. Partial on
-- `revoked_by is null` because a revoked punishment is never what any of those reads is looking for.
create index if not exists punishments_active_idx on punishments (player_id, type) where revoked_by is null;
