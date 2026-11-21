create table if not exists anticheat_traces
(
    id             text        not null primary key,
    capture_id     text,
    player_id      uuid        not null,
    proxy_version  text,                 -- the commit the proxy plugin was built from
    proxy          text,
    client_pvn      int         not null,
    reason         text check (reason in ('run', 'sample', 'flag', 'manual')),
    format_version int         not null, -- the container's, read off the uploaded bytes
    started_at     timestamptz not null,
    ended_at       timestamptz,          -- null for a trace cut short before it was closed
    bytes          bigint      not null,
    path           text        not null, -- relative to the store root: {yyyy}/{mm}/{dd}/{id}.trace
    pinned         bool        not null default false,
    expires_at     timestamptz,
    created_at     timestamptz not null default now()
);

create index if not exists anticheat_traces_capture_idx on anticheat_traces (capture_id) where capture_id is not null;
create index if not exists anticheat_traces_player_idx on anticheat_traces (player_id, started_at desc);
create index if not exists anticheat_traces_expires_idx on anticheat_traces (expires_at) where expires_at is not null and not pinned;
