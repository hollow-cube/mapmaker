-- Ours, not Go's: apply by hand.
--
-- Every command a player runs, from the one place a command is dispatched. Its own table rather
-- than a channel in `chat_messages` because none of these columns mean anything for a chat line and
-- none of that table's mean anything here, because commands outnumber chat by orders of magnitude
-- and would swamp the index built for "what did this player say", and because chat is moderation
-- evidence that stays while these are the rows a retention policy will eventually sweep.
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

-- The staff read: what one player ran, newest first.
create index if not exists command_log_player_idx on command_log (player_id, timestamp desc);
-- For whatever sweep this eventually gets. Rows arrive in timestamp order, which is exactly what
-- brin is for, and it costs a few pages rather than the size of the table.
create index if not exists command_log_timestamp_idx on command_log using brin (timestamp);
