-- The Go api-server's player sessions, as its `internal/db` migrations 000002, 000005, 000008 and
-- 000009 leave the table. Go owns the schema; this is what the queries here are described against,
-- and has to be kept in step with it by hand.
create table if not exists player_sessions
(
    player_id        uuid        not null primary key,
    created_at       timestamptz not null default now(),
    proxy_id         text        not null,
    server_id        text        default null,

    hidden           bool        not null default false,
    username         varchar(16) default null,
    skin_texture     text        not null,
    skin_signature   text        not null,

    -- Presence
    p_type           text        default null,
    p_state          text        default null,
    p_instance_id    text        default null,
    p_map_id         text        default null,
    p_start_time     timestamptz default null,

    last_seen        timestamptz default now(),
    protocol_version int         not null default 0
);
