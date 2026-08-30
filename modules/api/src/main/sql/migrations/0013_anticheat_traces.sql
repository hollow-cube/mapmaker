-- The index over the anticheat capture traces the proxy ships here: one row per `.trace` blob on
-- the shared volume, holding what the trace's own JSON header says about it so that finding a
-- capture never means opening files.
--
-- The blob is the record; this table is only the way in, and every column but `id` and `path` is a
-- copy of a header field. A row without its file is a bug worth seeing, which is why the file is
-- renamed into place before the row is written and never the other way round.
--
-- `id` is text rather than uuid because the proxy owns the name and a trace id has to survive
-- being a path component; `capture_id` is the backend's (a run id for competes) and is null for a
-- ring flush nobody asked for. `proxy_version` and `proxy` are null when the header was written by
-- a build that did not carry them yet, which is worth a row without them rather than no row.
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

-- Every trace of one capture, which is what the backend asks after a run.
create index if not exists anticheat_traces_capture_idx on anticheat_traces (capture_id) where capture_id is not null;
-- The staff read: what this player was doing, newest first.
create index if not exists anticheat_traces_player_idx on anticheat_traces (player_id, started_at desc);
-- For the sweeper there is no retention policy for yet. Partial because nothing sets expires_at
-- today, so the index costs a page rather than the size of the table until something does.
create index if not exists anticheat_traces_expires_idx on anticheat_traces (expires_at) where expires_at is not null and not pinned;
