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

