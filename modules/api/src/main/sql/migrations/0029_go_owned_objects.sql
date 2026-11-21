-- Objects the Go api-server created that no earlier migration mirrored, exactly as they are in
-- prod, so a database built from these migrations matches it. Prod already has all of this and is
-- baselined through this file rather than running it.
--
-- Two prod objects are left out because pglite, which sql-gen and the tests run on, cannot build
-- them: the plpgsql trigger map_ratings_update_likes_count (keeping maps.total_likes), and the
-- pg_trgm index idx_player_data_username_trgm.

alter table player_data add constraint hypercube_start_end_invariant
    check ((hypercube_start is null and hypercube_end is null) or (hypercube_start is not null and hypercube_end is not null));

--> statement-breakpoint
create index idx_maps_published_std_leaderboard on maps (id)
    where published_at is not null and deleted_at is null
        and coalesce(leaderboard ->> 'format', 'time') = 'time'
        and coalesce(leaderboard ->> 'asc', 'true') = 'true';

--> statement-breakpoint
create view maps_published as
select m.id,
       m.owner,
       m.m_type,
       m.created_at,
       m.updated_at,
       m.verification,
       m.authz_key,
       m.file_id,
       m.legacy_map_id,
       m.published_id,
       m.published_at,
       m.quality_override,
       m.opt_name,
       m.opt_icon,
       m.size,
       m.opt_variant,
       m.opt_subvariant,
       m.opt_spawn_point,
       m.opt_only_sprint,
       m.opt_no_sprint,
       m.opt_no_jump,
       m.opt_no_sneak,
       m.opt_boat,
       m.opt_extra,
       m.opt_tags,
       m.ext,
       m.deleted_at,
       m.deleted_by,
       m.deleted_reason,
       m.protocol_version,
       m.contest,
       m.listed,
       m.total_likes,
       m.leaderboard,
       array(select map_tags.tag from map_tags where map_tags.map_id = m.id order by map_tags.index) as tags,
       coalesce(stats.play_count, 0::bigint)                                                         as play_count,
       coalesce(stats.win_count, 0::bigint)                                                          as win_count,
       coalesce(stats.clear_rate, 0::double precision)                                               as clear_rate,
       case
           when stats.play_count < 10 then -1
           when stats.clear_rate < 0.05::double precision then 4
           when stats.clear_rate < 0.25::double precision then 3
           when stats.clear_rate < 0.5::double precision then 2
           when stats.clear_rate < 0.75::double precision then 1
           else 0
           end                                                                                       as difficulty
from maps m
         left join (select map_stats.map_id,
                           map_stats.play_count,
                           map_stats.win_count,
                           map_stats.win_count::double precision / nullif(map_stats.play_count, 0)::double precision as clear_rate
                    from map_stats) stats on m.id = stats.map_id
where m.deleted_at is null
  and m.published_at is not null
  and m.published_id is not null;

--> statement-breakpoint
create table map_states
(
    id     uuid not null primary key,
    map_id text not null,
    server text not null,
    state  text not null,
    constraint map_states_server_fkey foreign key (server) references server_states (id) on update cascade on delete cascade
);

--> statement-breakpoint
create table map_files
(
    map_id       uuid        not null,
    path         text        not null,
    content      bytea       not null,
    content_hash bytea       not null,
    content_type text        not null,
    size         integer generated always as (octet_length(content)) stored not null,
    updated_at   timestamptz not null default now(),
    created_at   timestamptz not null default now(),
    constraint map_files_pkey primary key (map_id, path),
    constraint map_files_map_id_fkey foreign key (map_id) references maps (id) on delete cascade,
    constraint content_hash_is_sha256 check (octet_length(content_hash) = 32),
    constraint path_valid check (path <> '' and path !~~ '/%' and path !~~ '%..%' and length(path) <= 512)
);

--> statement-breakpoint
-- Player

create table player_cosmetics
(
    player_id     uuid              not null,
    cosmetic_path character varying not null,
    unlocked_at   timestamptz       not null,
    constraint player_cosmetics_pkey primary key (player_id, cosmetic_path),
    constraint player_cosmetics_player_id_fkey foreign key (player_id) references player_data (id)
);

--> statement-breakpoint
create table player_recaps
(
    id        text    not null,
    player_id uuid    not null,
    year      integer not null,
    data      jsonb   not null,
    constraint player_recaps_pkey primary key (id),
    constraint player_recaps_player_id_fkey foreign key (player_id) references player_data (id)
);

--> statement-breakpoint
create table player_totp
(
    player_id      uuid        not null,
    created_at     timestamptz default now(),
    active         boolean     default false,
    key            bytea       not null,
    recovery_codes text[]      not null,
    constraint player_totp_pkey primary key (player_id),
    constraint player_totp_player_id_fkey foreign key (player_id) references player_data (id)
);

--> statement-breakpoint
create table linked_accounts
(
    player_id uuid              not null,
    social_id character varying not null,
    type      character varying not null,
    constraint linked_accounts_pkey primary key (type, social_id, player_id)
);

--> statement-breakpoint
create table pending_verification
(
    type        character varying not null,
    user_id     character varying not null,
    user_secret character varying not null,
    expiration  timestamptz       not null,
    constraint pending_verification_pkey primary key (type, user_id)
);

--> statement-breakpoint
-- API auth (v1)

create type api_client_kind as enum ('web', 'desktop');

--> statement-breakpoint
create table api_clients
(
    id           uuid            not null default gen_random_uuid(),
    kind         api_client_kind not null,
    public_key   bytea           not null,
    key_id       text            not null,
    label        text,
    created_at   timestamptz     not null default now(),
    last_seen_at timestamptz     not null default now(),
    constraint api_clients_pkey primary key (id),
    constraint api_clients_key_id_key unique (key_id)
);

--> statement-breakpoint
create table api_keys
(
    id         uuid        not null default gen_random_uuid(),
    key_hash   text        not null,
    player_id  uuid        not null,
    created_at timestamptz default now(),
    deleted_at timestamptz,
    constraint api_keys_pkey primary key (id),
    constraint api_keys_key_hash_key unique (key_hash),
    constraint api_keys_player_id_fkey foreign key (player_id) references player_data (id)
);

--> statement-breakpoint
create table api_sessions
(
    id                  uuid        not null default gen_random_uuid(),
    client_id           uuid        not null,
    player_id           uuid        not null,
    created_at          timestamptz not null default now(),
    last_active_at      timestamptz not null default now(),
    idle_expires_at     timestamptz not null,
    absolute_expires_at timestamptz not null,
    revoked_at          timestamptz,
    constraint api_sessions_pkey primary key (id),
    constraint api_sessions_client_id_fkey foreign key (client_id) references api_clients (id) on delete cascade,
    constraint api_sessions_player_id_fkey foreign key (player_id) references player_data (id) on delete cascade
);

--> statement-breakpoint
create table api_launch_grants
(
    id                  uuid        not null default gen_random_uuid(),
    code_hash           bytea       not null,
    player_id           uuid        not null,
    map_id              uuid,
    created_at          timestamptz not null default now(),
    expires_at          timestamptz not null,
    redeemed_at         timestamptz,
    redeemed_session_id uuid,
    constraint api_launch_grants_pkey primary key (id),
    constraint api_launch_grants_code_hash_key unique (code_hash),
    constraint api_launch_grants_player_id_fkey foreign key (player_id) references player_data (id) on delete cascade,
    constraint api_launch_grants_redeemed_session_id_fkey foreign key (redeemed_session_id) references api_sessions (id)
);

--> statement-breakpoint
-- Payments

create table pending_tebex_transactions
(
    player_id   character varying(36) not null,
    created_at  timestamptz           not null,
    username    character varying     not null,
    checkout_id character varying     not null,
    basket_id   character varying,
    constraint pending_tebex_transactions_pkey primary key (player_id, checkout_id)
);

--> statement-breakpoint
create table tebex_events
(
    id          serial            not null,
    event_id    character varying not null,
    "timestamp" timestamptz       not null,
    raw         bytea             not null,
    constraint tebex_events_pkey primary key (id)
);

--> statement-breakpoint
create table tebex_state
(
    tx_id    character varying not null,
    changes  jsonb             not null,
    reverted boolean           not null default false,
    constraint tebex_state_pkey primary key (tx_id)
);

--> statement-breakpoint
create table tx_log
(
    id          serial            not null,
    player_id   uuid              not null,
    "timestamp" timestamptz       not null,
    reason      character varying not null,
    currency    character varying not null default 'cubits',
    amount      bigint            not null,
    meta        jsonb,
    constraint tx_log_pkey primary key (id),
    constraint tx_log_player_id_fkey foreign key (player_id) references player_data (id)
);

--> statement-breakpoint
-- Terraform

create table tf_player_session
(
    player_id uuid  not null,
    state     bytea not null,
    constraint tf_player_session_pkey primary key (player_id)
);

--> statement-breakpoint
create table tf_local_session
(
    player_id uuid  not null,
    world_id  uuid  not null,
    state     bytea not null,
    constraint tf_local_session_pkey primary key (player_id, world_id)
);

--> statement-breakpoint
create table tf_schematics
(
    player_id  uuid                   not null,
    name       character varying      not null,
    dimensions bigint                 not null,
    size       bigint                 not null,
    schem_data bytea                  not null,
    filetype   character varying(255) not null default 'unknown',
    constraint tf_schematics_pkey primary key (player_id, name)
);
