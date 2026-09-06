-- Describe-time mirror of Go migrations, preserving physical column order. Nothing to apply.
create table if not exists maps
(
    id               uuid primary key,
    owner            uuid        not null,
    m_type           varchar     not null,
    created_at       timestamptz not null,
    updated_at       timestamptz not null,
    verification     int8                 default 0,
    authz_key        varchar              default null,
    file_id          varchar     not null,
    legacy_map_id    varchar              default null,

    published_id     bigint               default null,
    published_at     timestamptz          default null,

    quality_override int8                 default 0,

    opt_name         varchar              default null,
    opt_icon         varchar              default null,
    size             int8        not null default 0,
    opt_variant      varchar     not null,
    opt_subvariant   varchar              default null,
    opt_spawn_point  varchar     not null,

    opt_only_sprint  bool                 default false,
    opt_no_sprint    bool                 default false,
    opt_no_jump      bool                 default false,
    opt_no_sneak     bool                 default false,
    opt_boat         bool                 default false,
    opt_extra        bytea                default null,

    opt_tags         varchar[]            default null,

    ext              bytea       not null default '{}', -- holds the extended map data

    -- the following are only set if the map is soft deleted
    deleted_at       timestamptz          default null,
    deleted_by       uuid                 default null,
    deleted_reason   varchar              default null
);

alter table maps add column protocol_version int default 769;
alter table maps add column contest uuid default null;
alter table maps add column listed boolean not null default true;
alter table maps alter column opt_spawn_point type jsonb using opt_spawn_point::jsonb;
alter table maps alter column ext drop default;
alter table maps alter column ext type jsonb using convert_from(ext, 'UTF8')::jsonb;
alter table maps alter column ext set default '{}'::jsonb;
alter table maps add column total_likes int not null default 0;
alter table maps add column leaderboard jsonb default null;
