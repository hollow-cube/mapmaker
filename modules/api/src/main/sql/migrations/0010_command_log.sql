create table if not exists command_log
(
    id          bigint generated always as identity primary key,
    timestamp   timestamptz not null,          -- when it was submitted, off the server's clock
    player_id   uuid        not null,
    server_id   text        not null,
    map_id      text,                          -- null in the hub
    instance_id text,                          -- the world within the map, null in the hub
    command     text        not null,          -- what was typed, without the leading slash
    remote      bool        not null,          -- executed by the api rather than the server
    outcome     text        not null check (outcome in ('success', 'denied', 'not_found', 'syntax_error', 'execution_error')),
    error       text,                          -- the syntax message, or what was thrown
    duration_ms int         not null
);

create index if not exists command_log_player_idx on command_log (player_id, timestamp desc);
create index if not exists command_log_timestamp_idx on command_log using brin (timestamp);
