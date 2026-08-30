-- The Go api-server's maps, as its `internal/mapdb` migrations leave the table. Go owns the
-- schema; this is what the queries here are described against, and has to be kept in step with it
-- by hand. Nothing to apply.
--
-- Chat reads `published_id` off it, to answer whether the `[map]` someone typed is a map anyone
-- else can open. The rest is here so that the maps port has the table already described.
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
    opt_spawn_point  jsonb       not null,

    opt_only_sprint  bool                 default false,
    opt_no_sprint    bool                 default false,
    opt_no_jump      bool                 default false,
    opt_no_sneak     bool                 default false,
    opt_boat         bool                 default false,
    opt_extra        bytea                default null,

    opt_tags         varchar[]            default null,

    ext              jsonb       not null default '{}',

    protocol_version int                  default 769,
    contest          uuid                 default null,
    listed           boolean     not null default true,
    total_likes      int         not null default 0,
    leaderboard      jsonb                default null,

    -- the following are only set if the map is soft deleted
    deleted_at       timestamptz          default null,
    deleted_by       uuid                 default null,
    deleted_reason   varchar              default null
);
