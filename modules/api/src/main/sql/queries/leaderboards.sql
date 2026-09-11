-- The global boards and a player's top times, computed from `save_states` on every read as Go did.
-- A time here is playtime rounded to the tick, `greatest((playtime + 25) / 50, ticks) * 50`, which
-- is how two runs a few milliseconds apart tie. Integer arithmetic rather than Go's `round()`
-- because the embedded test engine cannot inline `round(numeric)`; for a non-negative playtime the
-- two agree.

-- name: topTimes :many
-- not-null: top_times
-- The players holding the fastest time on the most published, ascending time boards.
with shortest_playtimes as (select map_id,
                                   min((greatest((playtime + 25) / 50, ticks) * 50)::bigint) as min_playtime
                            from save_states
                                     join maps on save_states.map_id = maps.id
                            where deleted is null
                              and completed
                              and playtime != 0
                              and type in ('playing', 'verifying')
                              and maps.published_at is not null
                              and maps.deleted_at is null
                              and coalesce(maps.leaderboard ->> 'format', 'time') = 'time'
                              and coalesce(maps.leaderboard ->> 'asc', 'true') = 'true'
                            group by map_id)
select s1.player_id, count(distinct s1.map_id) as top_times
from shortest_playtimes
         join save_states s1
              on s1.map_id = shortest_playtimes.map_id
                  and (greatest((s1.playtime + 25) / 50, s1.ticks) * 50)::bigint = shortest_playtimes.min_playtime
where s1.deleted is null
  and s1.completed
group by s1.player_id
order by top_times desc
limit 10;

-- name: topTimesForPlayer :one
-- not-null: top_times
select count(distinct s1.map_id)::int as top_times
from save_states s1
         join maps on s1.map_id = maps.id
where s1.player_id = $playerId
  and s1.deleted is null
  and s1.completed
  and s1.playtime != 0
  and s1.type in ('playing', 'verifying')
  and maps.published_at is not null
  and maps.deleted_at is null
  and coalesce(maps.leaderboard ->> 'format', 'time') = 'time'
  and coalesce(maps.leaderboard ->> 'asc', 'true') = 'true'
  and not exists (select 1
                  from save_states s2
                  where s2.map_id = s1.map_id
                    and s2.deleted is null
                    and s2.completed
                    and s2.playtime != 0
                    and s2.type in ('playing', 'verifying')
                    and (greatest((s2.playtime + 25) / 50, s2.ticks) * 50)::bigint
                      < (greatest((s1.playtime + 25) / 50, s1.ticks) * 50)::bigint);

-- name: mapsBeaten :many
-- not-null: maps_beaten
select s1.player_id, count(distinct s1.map_id) as maps_beaten
from save_states s1
         join maps on s1.map_id = maps.id
where s1.deleted is null
  and s1.completed
  and s1.type in ('playing', 'verifying')
  and maps.published_at is not null
group by s1.player_id
order by maps_beaten desc
limit 10;

-- name: mapsBeatenForPlayer :one
-- not-null: maps_beaten
select count(distinct map_id)::int as maps_beaten
from save_states
         join maps on save_states.map_id = maps.id
where deleted is null
  and completed
  and type in ('playing', 'verifying')
  and player_id = $playerId
  and maps.published_at is not null;

-- name: playerBestTimes :many
-- The player's fastest run on every published map they have finished, by playtime rather than by
-- score, which is what the top-times rank is measured against.
select distinct on (save_states.map_id) save_states.map_id,
                                        save_states.playtime,
                                        maps.opt_name as map_name,
                                        maps.published_id
from save_states
         join maps on save_states.map_id = maps.id
where save_states.deleted is null
  and save_states.completed
  and save_states.player_id = $playerId
  and save_states.type in ('playing', 'verifying')
  and save_states.playtime != 0
  and maps.published_at is not null
  and maps.deleted_at is null
order by save_states.map_id, save_states.playtime;
