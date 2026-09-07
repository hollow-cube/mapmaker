-- The Go api-server's player data, transcribed from its `internal/playerdb` migrations in the
-- order they ran, because `select player_data.*` reads columns by position and the production table
-- has the columns in the order the alters added them. Go owns the schema; this has to be kept in
-- step with it by hand. It lives in the players database, not the one every other table here is in.

-- 000001_init (the columns that survive; `ip_history` was dropped by 000004 and its position is a
-- hole in production, which is why it is created and dropped here too).
create table if not exists player_data
(
    id           uuid          not null primary key,
    username     varchar       not null,
    first_join   timestamptz   not null,
    last_online  timestamptz   not null,
    playtime     bigint        not null default 0,
    experience   bigint        not null default 0,
    ip_history   varchar(15)[] not null default '{}',
    beta_enabled boolean                default false,
    settings     jsonb         not null default '{}',
    coins        bigint        not null default 0,
    cubits       bigint        not null default 0
);

-- 000004_remove_player_data_ip_history
alter table player_data drop column if exists ip_history;

-- 000010_player_skin_on_player_data
alter table player_data add column if not exists skin jsonb default null;

-- 000011_remote_player_online
alter table player_data add column if not exists online boolean not null default false;
alter table player_data alter column online drop default;

-- 000013_hypercube_on_player_data
alter table player_data add column if not exists hypercube_start timestamptz default null;
alter table player_data add column if not exists hypercube_end timestamptz default null;

create type role_type as enum (
    'default', 'hypercube', 'media',
    'ct_1', 'mod_1', 'dev_1',
    'ct_2', 'mod_2', 'dev_2',
    'ct_3', 'mod_3', 'dev_3'
    );
alter table player_data add column if not exists role role_type not null default 'default';

-- 000014_unlocks_on_pd
alter table player_data add column if not exists extra_map_slots int2 not null default 0;
alter table player_data add column if not exists max_map_size int2 not null default 0;

-- 000015_add_map_builders
alter table player_data add column if not exists map_builders int2 not null default 0;
