-- The Go api-server's player data, as its `internal/playerdb` migrations leave the table. Go owns
-- the schema; this is what the queries here are described against, and has to be kept in step with
-- it by hand. It lives in the players database, not the one every other table here is in.
--
-- Only `settings`, `role` and `hypercube_end` are read: whether a player takes direct messages, and
-- whether they have the extended limits that let them send the emoji everyone else cannot.
create type role_type as enum (
    'default', 'hypercube', 'media',
    'ct_1', 'mod_1', 'dev_1',
    'ct_2', 'mod_2', 'dev_2',
    'ct_3', 'mod_3', 'dev_3'
    );

create table if not exists player_data
(
    id              uuid        not null primary key,
    username        varchar     not null,
    first_join      timestamptz not null,
    last_online     timestamptz not null,
    playtime        bigint      not null default 0,
    experience      bigint      not null default 0,
    beta_enabled    boolean              default false,
    settings        jsonb       not null default '{}',
    skin            jsonb                default null,
    online          boolean     not null,

    coins           bigint      not null default 0,
    cubits          bigint      not null default 0,

    hypercube_start timestamptz          default null,
    hypercube_end   timestamptz          default null,
    role            role_type   not null default 'default',

    extra_map_slots int2        not null default 0,
    max_map_size    int2        not null default 0,
    map_builders    int2        not null default 0
);
