set local lock_timeout = '5s';

--> statement-breakpoint
alter table map_stats
    alter column play_count set default 0,
    alter column win_count set default 0,
    add column likes         integer not null default 0,
    add column est_length_ms bigint,
    add column refreshed_at  timestamptz;

--> statement-breakpoint
-- Likes net of dislikes, as `maps.total_likes` holds them.
insert into map_stats (map_id, likes)
select map_id, count(*) filter (where rating = 'liked') - count(*) filter (where rating = 'disliked')
from map_ratings
group by map_id
on conflict (map_id) do update
    set likes = excluded.likes;

--> statement-breakpoint
-- An unrated map is a null rating rather than no row, so taking a rating back moves `rated_at`.
alter table map_ratings
    alter column rating drop not null;

--> statement-breakpoint
create index idx_map_ratings_rated_at on map_ratings (rated_at);
