-- Expand step for the maps/save_states renames and retypes: the new columns are written alongside
-- the old ones and are not read yet. They are backfilled once nothing writes only the old ones.

alter table save_states
    add column created_at timestamptz,
    add column updated_at timestamptz,
    add column deleted_at timestamptz,
    add column state      jsonb;

--> statement-breakpoint
create type map_variant as enum ('parkour', 'building', 'adventure', 'freeform');

--> statement-breakpoint
create type map_subvariant as enum (
    'showcase', 'tutorial', 'speedrun', 'sectioned', 'rankup', 'gauntlet', 'dropper', 'one_jump',
    'informative');

--> statement-breakpoint
create type map_size as enum ('normal', 'large', 'massive', 'colossal', 'unlimited');

--> statement-breakpoint
create type map_verification as enum ('unverified', 'pending', 'verified');

--> statement-breakpoint
create type map_quality as enum ('unrated', 'good', 'great', 'excellent', 'outstanding', 'masterpiece');

--> statement-breakpoint
alter table maps
    add column name               text,
    add column icon               text,
    add column variant            map_variant,
    add column subvariant         map_subvariant,
    add column spawn_point        jsonb,
    add column extra              jsonb,
    add column size_class         map_size,
    add column verification_state map_verification,
    add column quality            map_quality,
    alter column m_type drop not null,
    alter column file_id drop not null;

--> statement-breakpoint
-- Ratings are swapped in place. UNRATED is now the absence of a row, and 6/7 were never valid.

delete
from map_ratings
where rating not in (1, 2);

--> statement-breakpoint
-- Go's trigger compares the rating to integers, so it cannot outlive the retype; the api-server
-- keeps `total_likes` itself from here on.
drop trigger if exists map_ratings_update_likes_count on map_ratings;

--> statement-breakpoint
drop function if exists update_map_likes_count();

--> statement-breakpoint
create type map_rating as enum ('liked', 'disliked');

--> statement-breakpoint
alter table map_ratings
    alter column rating type map_rating using (case rating when 1 then 'liked' else 'disliked' end)::map_rating;

--> statement-breakpoint
update maps m
set total_likes = coalesce((select count(*) filter (where r.rating = 'liked') - count(*) filter (where r.rating = 'disliked')
                            from map_ratings r
                            where r.map_id = m.id), 0)
where total_likes <> coalesce((select count(*) filter (where r.rating = 'liked') - count(*) filter (where r.rating = 'disliked')
                               from map_ratings r
                               where r.map_id = m.id), 0);

--> statement-breakpoint
create type map_report_category as enum (
    'cheated', 'discrimination', 'explicit_content', 'spam', 'dmca', 'troll', 'unplayable');

--> statement-breakpoint
-- A transform expression cannot hold a subquery, so the ordinal array is mapped by a function.
create function pg_temp.report_categories(ordinals int[]) returns map_report_category[]
    language sql
    immutable
as
$$
select array(select (enum_range(null::map_report_category))[o + 1]
             from unnest(ordinals) with ordinality as u(o, i)
             order by i)
$$;

--> statement-breakpoint
alter table map_reports
    alter column categories type map_report_category[] using pg_temp.report_categories(categories);

--> statement-breakpoint
drop function pg_temp.report_categories(int[]);

--> statement-breakpoint
-- The web script editor and its auth, gone from the Go api-server.

drop view maps_published;

--> statement-breakpoint
drop table map_files, api_launch_grants, api_sessions, api_clients;

--> statement-breakpoint
drop type api_client_kind;
