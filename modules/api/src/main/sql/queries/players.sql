-- Queries against player_data and punishments.

-- name: getChatPlayers :many
-- not-null: allow_dms, hypercube, muted
-- Everything chat asks about the people in a message, in one round trip: whether they take direct
-- messages, whether their emoji render for everyone, and whether they may talk at all.
--
-- Driven off the ids rather than off player_data, so every id asked about comes back whether or not
-- it has a row — a mute is on `punishments` and applies to someone this table has never seen.
-- `allow_direct_messages` is a key in the settings blob rather than a column, and defaults to on;
-- `hypercube` is Go's FlagExtendedLimits. The mute is Go's GetActivePunishment(type = 'mute'),
-- longest-lasting first since that is the one the player is actually under; `muted` is what says
-- there is one at all, because a permanent mute and no mute both have a null expiry.
select asked.id,
       coalesce((pd.settings ->> 'allow_direct_messages')::bool, true)     as allow_dms,
       coalesce(pd.role <> 'default' or pd.hypercube_end > now(), false)   as hypercube,
       mute.player_id is not null                                          as muted,
       mute.expires_at                                                     as mute_expires_at
from unnest($ids::uuid[]) as asked(id)
         left join player_data pd on pd.id = asked.id
         left join lateral (select p.player_id, p.expires_at
                            from punishments p
                            where p.player_id = asked.id::varchar
                              and p.type = 'mute'
                              and p.revoked_by is null
                              and (p.expires_at is null or p.expires_at > now())
                            order by p.expires_at desc nulls first
                            limit 1) mute on true;

-- name: getPlayerById :one
select player_data.*
from player_data
where id = $id;

-- name: getPlayerByUsername :one
select player_data.*
from player_data
where lower(username) = lower($username);

-- name: getPlayerNames :many
-- The columns a display name is computed from, for a batch of ids. Ids with no row are simply
-- absent; the caller's map says so.
select id, username, role, hypercube_end
from player_data
where id = any ($ids::uuid[]);

-- name: updatePlayerSettings :exec
-- One statement rather than read-modify-write, so two servers patching different keys both land.
-- Null deletes only a top-level key; nested objects remain opaque.
update player_data
set settings = (settings - coalesce((select array_agg(key)
                                     from jsonb_each($patch::jsonb)
                                     where value = 'null'::jsonb), '{}'::text[]))
    || coalesce((select jsonb_object_agg(key, value)
                 from jsonb_each($patch::jsonb)
                 where value <> 'null'::jsonb), '{}'::jsonb)
where id = $id;

-- name: searchPlayers :many
-- Substring match, prefix matches first, then the shorter name. `pg_trgm` is not available at
-- describe time so this is plain `like`; in production the planner still uses Go's trigram index
-- for the `%q%` predicate.
select player_data.*
from player_data
where id <> all ($exclude::uuid[])
  and lower(username) like '%' || lower($query::text) || '%'
order by (lower(username) like lower($query::text) || '%') desc, length(username), username
limit $limit;

-- name: getPlayerAlts :many
-- Everyone who has shared an address with the player.
select player_data.*
from player_data
where id in (select theirs.player_id
             from ip_history mine
                      join ip_history theirs on theirs.address = mine.address and theirs.player_id <> mine.player_id
             where mine.player_id = $playerId)
order by username;
