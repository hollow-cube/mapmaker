-- A player's editing or playing states on a map. Deletion is soft (`deleted`); the leaderboard
-- deletes and undeletes are here too, since striking a time is soft-deleting the runs behind it.
-- The score of a completed run is `score`, or playtime for runs from before boards were
-- configurable; every query that orders runs spells that fallback the same way Go does.

-- name: get :one
select save_states.*
from save_states
where deleted_at is null
  and id = $id
  and map_id = $mapId
  and player_id = $playerId;

-- name: latest :one
select save_states.*
from save_states
where deleted_at is null
  and map_id = $mapId
  and player_id = $playerId
  and type = $type
order by updated_at desc
limit 1;

-- name: best :one
select ss.*
from save_states ss
         join maps m on m.id = ss.map_id
where ss.deleted_at is null
  and ss.map_id = $mapId
  and ss.player_id = $playerId
  and ss.type in ('playing', 'verifying')
  and ss.completed
order by case
             when m.leaderboard is not null and (m.leaderboard ->> 'asc')::boolean = false
                 then -coalesce(ss.score, greatest(ss.playtime, ss.ticks * 50))
             else coalesce(ss.score, greatest(ss.playtime, ss.ticks * 50))
             end
limit 1;

-- name: bestPerPlayer :many
-- Every player's best completed run on the map, which is what its board holds.
select distinct on (ss.player_id) ss.*
from save_states ss
         join maps m on m.id = ss.map_id
where ss.deleted_at is null
  and ss.map_id = $mapId
  and ss.type in ('playing', 'verifying')
  and ss.completed
order by ss.player_id, case
                           when m.leaderboard is not null and (m.leaderboard ->> 'asc')::boolean = false
                               then -coalesce(ss.score, greatest(ss.playtime, ss.ticks * 50))
                           else coalesce(ss.score, greatest(ss.playtime, ss.ticks * 50))
                           end;

-- name: upsert :exec
-- nullable: $score
-- An id already held by another map's or player's row updates nothing, which the caller refuses.
insert into save_states (id, map_id, player_id, type, created_at, updated_at, completed, playtime, ticks,
                         score, state, data_version, protocol_version, resets, total_playtime)
values ($id, $mapId, $playerId, $type, $created, now(), $completed, $playtime, $ticks, $score, $state::jsonb,
        $dataVersion, $protocolVersion, $resets, $totalPlaytime)
on conflict (id) do update
    set updated_at       = excluded.updated_at,
        completed        = excluded.completed,
        playtime         = excluded.playtime,
        ticks            = excluded.ticks,
        score            = excluded.score,
        state            = excluded.state,
        data_version     = excluded.data_version,
        protocol_version = excluded.protocol_version,
        resets           = excluded.resets,
        total_playtime   = excluded.total_playtime
where save_states.map_id = excluded.map_id
  and save_states.player_id = excluded.player_id;

-- name: deleteForPlayer :exec
update save_states
set deleted_at = now()
where deleted_at is null
  and map_id = $mapId
  and player_id = $playerId;

-- name: deletePlayer :many
-- The maps touched, so their boards can drop the player.
with deleted as (
    update save_states
        set deleted_at = now()
        where deleted_at is null
            and player_id = $playerId
        returning map_id)
select distinct map_id
from deleted;

-- name: deleteMap :exec
update save_states
set deleted_at = now()
where deleted_at is null
  and map_id = $mapId;

-- name: undeleteForPlayer :one
-- nullable: $deletedAfter, $deletedBefore
-- not-null: restored
with restored as (
    update save_states
        set deleted_at = null
        where deleted_at is not null
            and map_id = $mapId
            and player_id = $playerId
            and ($deletedAfter::timestamptz is null or deleted_at >= $deletedAfter)
            and ($deletedBefore::timestamptz is null or deleted_at <= $deletedBefore)
        returning 1)
select count(*)::int as restored
from restored;

-- name: undeletePlayer :many
-- nullable: $deletedAfter, $deletedBefore
-- The maps touched, with how many runs came back on each.
with restored as (
    update save_states
        set deleted_at = null
        where deleted_at is not null
            and player_id = $playerId
            and ($deletedAfter::timestamptz is null or deleted_at >= $deletedAfter)
            and ($deletedBefore::timestamptz is null or deleted_at <= $deletedBefore)
        returning map_id)
select map_id, count(*)::int as restored
from restored
group by map_id;

-- name: undeleteMap :one
-- nullable: $deletedAfter, $deletedBefore
-- not-null: restored
with restored as (
    update save_states
        set deleted_at = null
        where deleted_at is not null
            and map_id = $mapId
            and ($deletedAfter::timestamptz is null or deleted_at >= $deletedAfter)
            and ($deletedBefore::timestamptz is null or deleted_at <= $deletedBefore)
        returning 1)
select count(*)::int as restored
from restored;
