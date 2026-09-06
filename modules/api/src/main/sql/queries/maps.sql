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
order by index;

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
-- The columns Go's `CreateDefaultMap` fills, with its values: `m_type`, `authz_key`, `file_id` and
-- `legacy_map_id` are dead but not null.
insert into maps (id, owner, m_type, created_at, updated_at, authz_key, file_id, legacy_map_id,
                  opt_name, opt_icon, opt_variant, opt_spawn_point, size, protocol_version)
values ($mapId, $owner, 'default', now(), now(), '', '', '', '', '', 'parkour',
        '{"x":0,"y":40,"z":0,"yaw":90,"pitch":0}', $size, $protocolVersion)
returning maps.*;

-- name: insertOwnerSlot :exec
-- The owner's own slot has index -1; bought slots count up from 0.
insert into map_slots (player_id, map_id, index)
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
update maps
set opt_name         = $name,
    opt_icon         = $icon,
    size             = $size,
    opt_variant      = $variant,
    opt_subvariant   = $subvariant,
    opt_spawn_point  = $spawnPoint,
    leaderboard      = $leaderboard,
    opt_extra        = $extra,
    opt_only_sprint  = $onlySprint,
    opt_no_sprint    = $noSprint,
    opt_no_jump      = $noJump,
    opt_no_sneak     = $noSneak,
    opt_boat         = $boat,
    listed           = $listed,
    quality_override = $quality,
    protocol_version = $protocolVersion,
    updated_at       = now()
where id = $mapId;

-- name: deleteTags :exec
delete
from map_tags
where map_id = $mapId;

-- name: insertTags :exec
insert into map_tags (map_id, tag, index)
select $mapId, tag::map_tag, ordinality - 1
from unnest($tags::text[]) with ordinality as tags(tag, ordinality);

-- name: listKnownTags :many
select unnest(enum_range(null::map_tag))::text as tag;

-- name: deleteInProgressStates :exec
-- A completed run is somebody's record and stays.
update save_states
set deleted = now()
where map_id = $mapId
  and deleted is null
  and not completed
  and type in ('playing', 'verifying');

-- name: deleteAllStates :exec
update save_states
set deleted = now()
where map_id = $mapId
  and deleted is null;

-- name: deleteVerifyingStates :exec
update save_states
set deleted = now()
where map_id = $mapId
  and type = 'verifying'
  and deleted is null;

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

-- name: removeLegacyBuilders :exec
delete
from map_builders
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
set verification = $verification
where id = $mapId;

-- name: getLatestEditingTime :one
select playtime
from save_states
where map_id = $mapId
  and player_id = $playerId
  and type = 'editing'
  and deleted is null
order by updated desc
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
  and opt_variant in ('parkour', 'building')
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
insert into map_ratings (map_id, player_id, rating)
values ($mapId, $playerId, $rating)
on conflict (map_id, player_id) do update
    set rating = excluded.rating;

-- name: insertReport :exec
insert into map_reports (map_id, player_id, time, categories, comment)
values ($mapId, $playerId, now(), $categories, $comment);
