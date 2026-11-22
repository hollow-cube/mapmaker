-- name: isMapPublished :one
-- not-null: published
-- Whether `[map]` in a message is a map the people reading it could open. Matches what a map server
-- used to answer for itself out of `MapData.isPublished`.
select count(*) > 0 as published
from maps
where id = $mapId
  and published_id is not null;

-- name: getMap :one
select maps.*
from maps
where id = $mapId
  and deleted_at is null;

-- name: getMapForUpdate :one
-- The lock every map write takes first. Preconditions are checked in Java against the locked row.
select maps.*
from maps
where id = $mapId
  and deleted_at is null
    for update;

-- name: getMapIncludingDeleted :one
select maps.*
from maps
where id = $mapId;

-- name: getMapByPublishedId :one
select maps.*
from maps
where published_id = $publishedId
  and published_at is not null
  and deleted_at is null;

-- name: getTags :many
select tag::text as tag
from map_tags
where map_id = $mapId
order by position;

-- name: getStats :one
select map_stats.*
from map_stats
where map_id = $mapId;

-- name: getPlayerForUpdate :one
-- The row lock is what makes "count the slots, then take one" atomic: two creates, or a create and
-- an accepted invitation, for the same player queue here instead of both seeing the last free slot.
-- Go read the count unlocked and could hand out one more than the limit.
select id, role, hypercube_end, extra_map_slots, max_map_size, map_builders, settings
from player_data
where id = $playerId
    for update;

-- name: getPlayersForUpdate :many
-- As getPlayerForUpdate, for the owner and the invitee together; ordered so that two invitations
-- between the same pair cannot deadlock.
select id, role, hypercube_end, extra_map_slots, max_map_size, map_builders, settings
from player_data
where id = any($ids::uuid[])
order by id
    for update;

-- name: createMap :one
insert into maps (id, owner, created_at, updated_at, name, icon, variant, spawn_point, size_class,
                  verification_state, quality, protocol_version)
values ($mapId, $owner, now(), now(), '', '', 'parkour', '{"x":0,"y":40,"z":0,"yaw":90,"pitch":0}', $size,
        'unverified', 'unrated', $protocolVersion)
returning maps.*;

-- name: insertOwnerSlot :exec
-- The owner's own slot has position -1; bought slots count up from 0.
insert into map_slots (player_id, map_id, position)
values ($playerId, $mapId, -1);

-- name: countSlots :one
-- not-null: count
-- Slots in use: every unpublished map the player owns or has accepted an invitation to.
select count(*)::int as count
from map_slots s
         join maps m on m.id = s.map_id
where s.player_id = $playerId
  and not s.is_pending
  and m.deleted_at is null
  and m.published_at is null;

-- name: updateMap :exec
-- nullable: $subvariant
update maps
set name             = $name,
    icon             = $icon,
    size_class       = $size,
    variant          = $variant,
    subvariant       = $subvariant,
    spawn_point      = $spawnPoint,
    leaderboard      = $leaderboard,
    extra            = $extra::jsonb,
    opt_only_sprint  = $onlySprint,
    opt_no_sprint    = $noSprint,
    opt_no_jump      = $noJump,
    opt_no_sneak     = $noSneak,
    opt_boat         = $boat,
    listed           = $listed,
    quality          = $quality,
    protocol_version = $protocolVersion,
    updated_at       = now()
where id = $mapId;

-- name: deleteTags :exec
delete
from map_tags
where map_id = $mapId;

-- name: insertTags :exec
insert into map_tags (map_id, tag, position)
select $mapId, tag::map_tag, ordinality - 1
from unnest($tags::text[]) with ordinality as tags(tag, ordinality);

-- name: listKnownTags :many
select unnest(enum_range(null::map_tag))::text as tag;

-- name: deleteInProgressStates :exec
-- A completed run is somebody's record and stays.
update save_states
set deleted_at = now()
where map_id = $mapId
  and deleted_at is null
  and not completed
  and type in ('playing', 'verifying');

-- name: deleteAllStates :exec
update save_states
set deleted_at = now()
where map_id = $mapId
  and deleted_at is null;

-- name: deleteVerifyingStates :exec
update save_states
set deleted_at = now()
where map_id = $mapId
  and type = 'verifying'
  and deleted_at is null;

-- name: deleteMap :exec
update maps
set deleted_at     = now(),
    deleted_by     = $actorId,
    deleted_reason = $reason
where id = $mapId;

-- name: removeSlots :exec
delete
from map_slots
where map_id = $mapId;

-- name: publishMap :one
update maps
set published_id = $publishedId,
    published_at = now(),
    updated_at   = now()
where id = $mapId
returning maps.*;

-- name: updateVerification :exec
update maps
set verification_state = $verification
where id = $mapId;

-- name: markVerified :exec
-- A verifying run that finished; the map is stamped with the version that verified it.
update maps
set verification_state = $verification,
    protocol_version   = $protocolVersion
where id = $mapId;

-- name: getLatestEditingTime :one
select playtime
from save_states
where map_id = $mapId
  and player_id = $playerId
  and type = 'editing'
  and deleted_at is null
order by updated_at desc
limit 1;

-- name: countWorlds :one
-- not-null: count
-- Editors still registered for the map; zero once a drain has completed.
select count(*)::int as count
from map_worlds
where map_id = $mapId;

-- name: listBuilders :many
select map_slots.*
from map_slots
where map_id = $mapId
order by created_at, player_id;

-- name: listPlayerSlots :many
select s.*
from map_slots s
         join maps m on m.id = s.map_id
where s.player_id = $playerId
  and not s.is_pending
  and m.deleted_at is null
  and m.published_at is null
order by s.created_at desc;

-- name: listPlayerPublishedMaps :many
select maps.*
from maps
where owner = $playerId
  and published_at is not null
  and deleted_at is null
  and listed
  and variant in ('parkour', 'building')
order by published_at desc;

-- name: getBuilderSlot :one
select map_slots.*
from map_slots
where map_id = $mapId
  and player_id = $playerId;

-- name: inviteBuilder :exec
insert into map_slots (map_id, player_id, is_pending)
values ($mapId, $playerId, true);

-- name: acceptBuilder :exec
update map_slots
set is_pending = false
where map_id = $mapId
  and player_id = $playerId;

-- name: removeBuilder :exec
delete
from map_slots
where map_id = $mapId
  and player_id = $playerId;

-- name: deleteNotifications :many
-- Every notification of one type under a key, or one player's when `playerId` is given; the rows
-- come back so their deletion can be published.
update player_notifications
set deleted_at = now()
where type = $type
  and key = $key
  and deleted_at is null
  and ($playerId::uuid is null or player_id = $playerId::uuid)
returning player_notifications.*;

-- name: insertNotification :exec
insert into player_notifications (id, player_id, type, key, data)
values ($id, $playerId, $type, $key, $data);

-- name: getRating :one
select rating
from map_ratings
where map_id = $mapId
  and player_id = $playerId;

-- name: setRating :exec
-- nullable: $rating
-- Null is unrated.
insert into map_ratings (map_id, player_id, rating)
values ($mapId, $playerId, $rating)
on conflict (map_id, player_id) do update
    set rating   = excluded.rating,
        rated_at = now();

-- name: insertReport :exec
insert into map_reports (map_id, player_id, created_at, categories, comment)
values ($mapId, $playerId, now(), $categories, $comment);

-- name: searchMaps :many
-- not-null: tags, play_count, win_count, total_likes, total_count
-- Go's `maps_published` view (`map_stats` join, difficulty bucket) written out so the row keeps the
-- maps table record. The listing predicate and the caller's filters are the `where` fragment and the
-- sort the `order by` fragment, both built in `MapSearchSql`.
select maps.*,
       array(select tag::text from map_tags where map_id = maps.id order by position) as tags,
       coalesce(stats.play_count, 0)                                                 as play_count,
       coalesce(stats.win_count, 0)                                                  as win_count,
       coalesce(stats.likes, 0)                                                      as total_likes,
       count(*) over ()                                                              as total_count
from maps
         left join map_stats stats on stats.map_id = maps.id
/* where */
/* order by */
offset $offset limit $limit;

-- name: getMultiMapProgress :many
-- not-null: completed, playtime
-- Go's `GetMultiMapProgress`: complete if any playing or verifying run finished, and the playtime
-- of the best finished run, else of the run touched last. Playtime is tick-rounded milliseconds;
-- Go's `round(playtime / 50.0)` is spelt as integer arithmetic because pglite cannot inline `round`.
with runs as (select ss.map_id,
                     ss.completed,
                     (greatest((ss.playtime + 25) / 50, ss.ticks) * 50)::int8 as playtime,
                     ss.updated_at
              from save_states ss
              where ss.player_id = $playerId
                and ss.map_id = any ($mapIds::uuid[])
                and ss.deleted_at is null
                and ss.type in ('playing', 'verifying'))
select map_id,
       bool_or(completed) as completed,
       case
           when bool_or(completed) then min(playtime) filter (where completed)
           else (select r.playtime from runs r where r.map_id = runs.map_id order by r.updated_at desc limit 1)
           end            as playtime
from runs
group by map_id;

-- name: listPlayerHistory :many
-- not-null: tags, play_count, win_count, total_likes, total_count
-- Maps drop out here once deleted instead of 404ing the page in the client.
select maps.*,
       array(select tag::text from map_tags where map_id = maps.id order by position) as tags,
       coalesce(stats.play_count, 0)                                                 as play_count,
       coalesce(stats.win_count, 0)                                                  as win_count,
       coalesce(stats.likes, 0)                                                      as total_likes,
       count(*) over ()                                                              as total_count
from (select map_id, max(updated_at) as last_played
      from save_states
      where player_id = $playerId
        and type = 'playing'
        and deleted_at is null
      group by map_id) recent
         join maps on maps.id = recent.map_id and maps.deleted_at is null
         left join map_stats stats on stats.map_id = maps.id
order by recent.last_played desc
offset $offset limit $limit;
