-- Queries against player_data and punishments.

-- name: getChatPlayers :many
-- not-null: id, allow_dms, hypercube, muted
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
