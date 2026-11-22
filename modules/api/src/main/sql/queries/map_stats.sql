-- name: lastRefresh :one
select max(refreshed_at) as last
from map_stats;

-- name: changedSince :many
-- Deleting or restoring a run leaves `updated_at` alone, so those paths refresh their maps
-- themselves.
select map_id
from save_states
where updated_at > $since
union
select map_id
from map_ratings
where rated_at > $since;

-- name: listAll :many
select map_id
from save_states
union
select map_id
from map_ratings;

-- name: refresh :exec
insert into map_stats (map_id, play_count, win_count, likes, est_length_ms, refreshed_at)
-- a median of first clear over verify time on all maps with 20+ first clears.
-- up-to-date as of 2026-11-22, should be refreshed occasionally
with params as (select 1.03::float8 as length_ratio),
     targets as (select unnest($mapIds::uuid[]) as map_id),
     ratings as (select map_id,
                        count(*) filter (where rating = 'liked') - count(*) filter (where rating = 'disliked') as likes
                 from map_ratings
                 where map_id = any ($mapIds::uuid[])
                 group by map_id),
     counts as (select map_id,
                       count(distinct player_id)                          as plays,
                       count(distinct player_id) filter (where completed) as wins
                from save_states
                where map_id = any ($mapIds::uuid[])
                  and type = 'playing'
                  and deleted_at is null
                group by map_id),
     -- Everyones first clear time including resets (for runs that have them)
     first_clears as (select distinct on (map_id, player_id) map_id, greatest(total_playtime, playtime) as ms
                      from save_states
                      where map_id = any ($mapIds::uuid[])
                        and type = 'playing'
                        and completed
                        and playtime > 0
                        and deleted_at is null
                      order by map_id, player_id, created_at),
     -- Median of everyones first clears
     evidence as (select map_id, count(*) as clears, percentile_cont(0.5) within group (order by ms) as median_ms
                  from first_clears
                  group by map_id),
     -- single author verification state (including reset time)
     verify as (select distinct on (map_id) map_id, greatest(total_playtime, playtime) as ms
                from save_states
                where map_id = any ($mapIds::uuid[])
                  and type = 'verifying'
                  and completed
                  and playtime > 0
                order by map_id, created_at desc)
select targets.map_id,
       coalesce(counts.plays, 0),
       coalesce(counts.wins, 0),
       coalesce(ratings.likes, 0),
       case
           -- If we dont have a verification (deleted) we have to use the median firsts
           when verify.ms is null then evidence.median_ms
           -- If we dont have a median (just published), use verification
           when evidence.median_ms is null then verify.ms * params.length_ratio
           -- Otherwise a weighted geometric mean favoring verification on low clear rate otherwise the
           -- actual first completion median. In practice the verification time * length ratio is fairly
           -- close to the 20+ first completion median so its a decent predictor until we have a lot of
           -- runs to compare against
           else exp((10 * ln(verify.ms * params.length_ratio) + evidence.clears * ln(evidence.median_ms))
               / (10 + evidence.clears))
           end::int8,
       now()
from targets
         cross join params
         left join counts on counts.map_id = targets.map_id
         left join ratings on ratings.map_id = targets.map_id
         left join evidence on evidence.map_id = targets.map_id
         left join verify on verify.map_id = targets.map_id
on conflict (map_id) do update
    set play_count    = excluded.play_count,
        win_count     = excluded.win_count,
        likes         = excluded.likes,
        est_length_ms = excluded.est_length_ms,
        refreshed_at  = excluded.refreshed_at;
