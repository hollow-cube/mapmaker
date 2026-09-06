-- Describe-time mirror of existing Go objects. Nothing to apply.
create table if not exists save_states
(
    id        uuid        not null,
    map_id    uuid        not null,
    player_id uuid        not null,
    type      varchar     not null,
    created   timestamptz not null,
    updated   timestamptz not null,
    deleted   timestamptz default null,
    completed boolean     not null,
    playtime  bigint      not null,

    state_v2  bytea       not null, -- Holds either editing or playing state, depending on the type, as json right now

    primary key (id, map_id, player_id),
    constraint fk_map_id foreign key (map_id) references maps (id)
);
create table if not exists map_ratings
(
    map_id    uuid not null references maps (id),
    player_id uuid not null, -- does not reference map_player_data because entries in that table are lazy.
    rating    int  not null, -- 0 = dislike, like = 1
    comment   varchar default null,

    primary key (map_id, player_id)
);
create table if not exists map_stats
(
    map_id     uuid   not null primary key references maps (id),
    play_count bigint not null,
    win_count  bigint not null
);
create table if not exists map_reports
(
    id         serial      not null primary key,
    map_id     uuid        not null references maps (id),
    player_id  uuid        not null, -- does not reference map_player_data because entries in that table are lazy.
    time       timestamptz not null,
    categories int[]       not null,
    comment    varchar default null
);
alter table map_stats add column clear_rate float8 generated always as (win_count::float8 / nullif(play_count, 0)::float8) stored;
alter table save_states add column data_version integer not null default 0;
alter table save_states add column protocol_version int default 769;
alter table save_states add column ticks integer not null default 0;
create type save_state_type as enum ('editing', 'playing', 'verifying');
alter table save_states alter column type type save_state_type using type::save_state_type;
alter table save_states add column score double precision default null;
alter table save_states add column resets integer not null default 0;
alter table save_states add column total_playtime bigint not null default 0;
create type map_tag as enum ('autocomplete', 'bossbattle', 'escape', 'exploration', 'interior', 'organics', 'puzzle', 'recreation', 'story', 'strategy', 'structure', 'terrain', 'trivia', 'twodimensional');



create table if not exists map_tags
(
  map_id uuid    not null references maps (id) on delete cascade,
  tag    map_tag not null,
  primary key (map_id, tag)
);

alter type map_tag add value 'music';
alter type map_tag add value 'coop';
alter type map_tag add value 'minigame';
alter type map_tag add value 'speedrun';
alter type map_tag add value 'sectioned';
alter type map_tag add value 'rankup';
alter type map_tag add value 'gauntlet';
alter type map_tag add value 'dropper';
alter type map_tag add value 'one_jump';
alter type map_tag add value 'tutorial';
alter type map_tag add value 'timed';
alter type map_tag add value 'only_sprint';
alter type map_tag add value 'no_sprint';
alter type map_tag add value 'no_sneak';
alter type map_tag add value 'no_jump';
alter type map_tag add value 'no_turning';
alter type map_tag add value 'block_placing';
alter type map_tag add value 'elytra';
alter type map_tag add value 'trident';
alter type map_tag add value 'mace';
alter type map_tag add value 'spear';
alter type map_tag add value 'ender_pearl';
alter type map_tag add value 'wind_charge';

alter table map_tags add column index int;
alter table map_tags alter column index set not null;


create table if not exists map_slots
(
  player_id  uuid        not null,
  map_id     uuid        not null references maps (id) on delete cascade,
  index      int         not null default -1,
  created_at timestamptz not null default now(),
  primary key (player_id, map_id)
);

create index if not exists idx_map_slots_player_id on map_slots (player_id);
create unique index idx_map_slots_player_index_unique
  on map_slots (player_id, index)
  where index >= 0;



create table if not exists map_builders
(
  map_id     uuid        not null references maps (id) on delete cascade,
  player_id  uuid        not null,
  created_at timestamptz not null default now(),
  is_pending bool                 default true,
  primary key (map_id, player_id)
);



alter table map_slots add column is_pending bool not null default false;
-- Go's map_ratings_update_likes_count trigger remains installed in production.
-- pglite4j has no plpgsql; its unchanged body is exercised by the PostgreSQL integration test.
create table if not exists server_states
(
    id         text        not null primary key,
    role       text        not null,
    start_time timestamptz not null default now(),
    status     int         not null default 0,
    cluster_ip text        not null default ''
);
create table if not exists map_worlds
(
  id         text        not null primary key,
  map_id     uuid        not null references maps (id) on delete cascade,
  server_id  text        not null references server_states (id) on delete cascade,
  created_at timestamptz not null default now()
);
create table if not exists player_notifications
(
    id         uuid primary key,
    player_id  uuid        not null references player_data (id),
    type       varchar     not null,
    key        varchar     not null,
    data       jsonb                default null,
    created_at timestamptz not null default now(),
    read_at    timestamptz          default null,
    expires_at timestamptz          default null,
    deleted_at timestamptz          default null
);
