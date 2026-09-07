-- The Go api-server's ip history and relationship tables, as its `internal/playerdb` migrations
-- 000003 and 000008 leave them (`player_notifications` is mirrored in 0016). Go owns the schema; this is what the queries
-- here are described against, and has to be kept in step with it by hand.
--
-- The legacy ip_history timestamps are converted to timestamptz in 0020.
create table if not exists ip_history
(
    player_id  uuid      not null references player_data (id),
    address    text      not null,
    first_seen timestamp not null,
    last_seen  timestamp not null,
    seen_count integer   not null,

    primary key (player_id, address)
);

create table if not exists player_friends
(
    player_id  uuid        not null references player_data (id) on delete cascade,
    target_id  uuid        not null references player_data (id) on delete cascade,

    created_at timestamptz not null default now(),

    primary key (player_id, target_id),
    check ( player_id <> target_id )
);

create table if not exists player_friend_requests
(
    player_id  uuid        not null references player_data (id) on delete cascade,
    target_id  uuid        not null references player_data (id) on delete cascade,

    created_at timestamptz not null default now(),

    primary key (player_id, target_id),
    check ( player_id <> target_id )
);

create table if not exists player_blocks
(
    player_id  uuid        not null references player_data (id) on delete cascade,
    target_id  uuid        not null references player_data (id) on delete cascade,

    created_at timestamptz not null default now(),

    primary key (player_id, target_id),
    check ( player_id <> target_id )
);

