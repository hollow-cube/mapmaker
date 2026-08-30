-- The Go api-server's chat log, as its `internal/db` migration 000001 leaves the table. Go owns
-- the schema; this is what the queries here are described against, and has to be kept in step with
-- it by hand.
--
-- No key and no index: `channel` is 'global'/'local'/'staff' or, for a direct message, the target's
-- uuid, and `target` has never been written by anything. 0007 adds the one index a reader needs.
create table if not exists chat_messages
(
    timestamp       timestamptz not null,
    server_id       varchar(50) not null,
    channel         varchar(50) not null,
    sender          varchar(36) not null,
    target          varchar(50) default null,
    content         text        not null,

    censored_by     varchar(36) default null,
    censored_detail text        default null
);
