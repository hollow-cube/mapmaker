-- A player's editing or playing states on a map. Deletion is soft (`deleted`); the leaderboard
-- deletes and undeletes are here too, since striking a time is soft-deleting the runs behind it.
-- The score of a completed run is `score`, or playtime for runs from before boards were
-- configurable; every query that orders runs spells that fallback the same way Go does.

-- name: get :one
select save_states.*
from save_states
where deleted is null
  and id = $id
  and map_id = $mapId
  and player_id = $playerId;

-- name: latest :one
select save_states.*
from save_states
where deleted is null
  and map_id = $mapId
  and player_id = $playerId
  and type = $type
order by updated desc
limit 1;

-- name: best :one
select ss.*
from save_states ss
         join maps m on m.id = ss.map_id
where ss.deleted is null
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
where ss.deleted is null
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
insert into save_states (id, map_id, player_id, type, created, updated, completed, playtime, ticks, score,
                         state_v2, data_version, protocol_version, resets, total_playtime)
values ($id, $mapId, $playerId, $type, $created, now(), $completed, $playtime, $ticks, $score, $state,
        $dataVersion, $protocolVersion, $resets, $totalPlaytime)
on conflict (id, map_id, player_id) do update
    set updated          = excluded.updated,
        completed        = excluded.completed,
        playtime         = excluded.playtime,
        ticks            = excluded.ticks,
        score            = excluded.score,
        state_v2         = excluded.state_v2,
        data_version     = excluded.data_version,
        protocol_version = excluded.protocol_version,
        resets           = excluded.resets,
        total_playtime   = excluded.total_playtime;

-- name: refreshStats :exec
-- `map_stats` counts every player who ever had a state on the map, deleted ones included, and
-- every one of them who finished; `clear_rate` is generated from the two.
insert into map_stats (map_id, play_count, win_count)
select $mapId,
       count(distinct player_id),
       count(distinct case when completed then player_id end)
from save_states
where map_id = $mapId
on conflict (map_id) do update
    set play_count = excluded.play_count,
        win_count  = excluded.win_count;

-- name: deleteForPlayer :exec
update save_states
set deleted = now()
where deleted is null
  and map_id = $mapId
  and player_id = $playerId;

-- name: deletePlayer :many
-- The maps touched, so their boards can drop the player.
with deleted as (
    update save_states
        set deleted = now()
        where deleted is null
            and player_id = $playerId
        returning map_id)
select distinct map_id
from deleted;

-- name: deleteMap :exec
update save_states
set deleted = now()
where deleted is null
  and map_id = $mapId;

-- name: undeleteForPlayer :one
-- nullable: $deletedAfter, $deletedBefore
-- not-null: restored
with restored as (
    update save_states
        set deleted = null
        where deleted is not null
            and map_id = $mapId
            and player_id = $playerId
            and ($deletedAfter::timestamptz is null or deleted >= $deletedAfter)
            and ($deletedBefore::timestamptz is null or deleted <= $deletedBefore)
        returning 1)
select count(*)::int as restored
from restored;

-- name: undeletePlayer :many
-- nullable: $deletedAfter, $deletedBefore
-- The maps touched, with how many runs came back on each.
with restored as (
    update save_states
        set deleted = null
        where deleted is not null
            and player_id = $playerId
            and ($deletedAfter::timestamptz is null or deleted >= $deletedAfter)
            and ($deletedBefore::timestamptz is null or deleted <= $deletedBefore)
        returning map_id)
select map_id, count(*)::int as restored
from restored
group by map_id;

-- name: undeleteMap :one
-- nullable: $deletedAfter, $deletedBefore
-- not-null: restored
with restored as (
    update save_states
        set deleted = null
        where deleted is not null
            and map_id = $mapId
            and ($deletedAfter::timestamptz is null or deleted >= $deletedAfter)
            and ($deletedBefore::timestamptz is null or deleted <= $deletedBefore)
        returning 1)
select count(*)::int as restored
from restored;
