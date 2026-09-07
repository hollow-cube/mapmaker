-- Friends, friend requests and blocks. Friendships are stored twice, one row per direction, which
-- is how Go's handlers and the website read them; requests and blocks once.
--
-- `online` is not a column here. It is derived from player_sessions — a visible session is being
-- online — because `player_data.online` is a cache Go's session handlers keep that only existed
-- while players and sessions were in different databases.

-- name: getFriends :many
-- not-null: total_count, online
-- Online friends first, most recently befriended first among them; offline friends by when they
-- were last seen, which is Go's ordering.
with friends as (select pf.target_id,
                        pf.created_at,
                        exists (select 1
                                from player_sessions s
                                where s.player_id = pf.target_id
                                  and not s.hidden) as online
                 from player_friends pf
                 where pf.player_id = $playerId)
select count(*) over () as total_count,
       player.*,
       f.online,
       f.created_at   as friends_since
from friends f
         join player_data player on player.id = f.target_id
where not $onlineOnly::bool
   or f.online
order by f.online desc,
         case when f.online then f.created_at else player.last_online end desc
offset $offset limit $limit;

-- name: getOnlineFriendIds :many
select pf.target_id
from player_friends pf
         join player_sessions s on s.player_id = pf.target_id and not s.hidden
where pf.player_id = $playerId;

-- name: getFriendUsage :one
-- not-null: friend_count, outgoing_request_count
select (select count(*) from player_friends where player_id = $playerId)::int         as friend_count,
       (select count(*) from player_friend_requests where player_id = $playerId)::int as outgoing_request_count;

-- name: isFriend :one
-- not-null: is_friend
select exists(select 1
              from player_friends
              where player_id = $playerId
                and target_id = $targetId) as is_friend;

-- name: insertFriendship :exec
-- Both directions at once; `on conflict do nothing` makes a retried accept harmless.
insert into player_friends (player_id, target_id)
values ($playerId, $targetId),
       ($targetId, $playerId)
on conflict do nothing;

-- name: deleteFriendship :exec
delete
from player_friends
where (player_id = $playerId and target_id = $targetId)
   or (player_id = $targetId and target_id = $playerId);

-- name: getIncomingFriendRequests :many
-- not-null: total_count
select count(*) over () as total_count,
       player.*,
       pfr.created_at   as sent_at
from player_friend_requests pfr
         join player_data player on player.id = pfr.player_id
where pfr.target_id = $playerId
order by pfr.created_at desc
offset $offset limit $limit;

-- name: getOutgoingFriendRequests :many
-- not-null: total_count
select count(*) over () as total_count,
       player.*,
       pfr.created_at   as sent_at
from player_friend_requests pfr
         join player_data player on player.id = pfr.target_id
where pfr.player_id = $playerId
order by pfr.created_at desc
offset $offset limit $limit;

-- name: hasFriendRequest :one
-- not-null: has_request
select exists(select 1
              from player_friend_requests
              where player_id = $playerId
                and target_id = $targetId) as has_request;

-- name: insertFriendRequest :exec
-- Rows affected says whether it was new; a duplicate is an outcome, not an exception.
insert into player_friend_requests (player_id, target_id)
values ($playerId, $targetId)
on conflict do nothing;

-- name: deleteFriendRequests :many
-- The player's request to the target, and with `bidirectional` the target's to the player.
-- Returns what went, with the sender so the caller knows which way each pointed.
delete
from player_friend_requests
where (player_id = $playerId and target_id = $targetId)
   or ($bidirectional::bool and player_id = $targetId and target_id = $playerId)
returning player_friend_requests.*;

-- name: isBlocked :one
-- not-null: is_blocked
select exists(select 1
              from player_blocks
              where player_id = $playerId
                and target_id = $targetId) as is_blocked;

-- name: insertBlock :exec
insert into player_blocks (player_id, target_id)
values ($playerId, $targetId)
on conflict do nothing;

-- name: deleteBlock :exec
delete
from player_blocks
where player_id = $playerId
  and target_id = $targetId;

-- name: getBlocks :many
-- not-null: total_count
-- Staff are filtered here rather than after paging, so the page is full and the total is the
-- number of lines the player will see.
select count(*) over () as total_count,
       player.*,
       pb.created_at    as blocked_at
from player_blocks pb
         join player_data player on player.id = pb.target_id
where pb.player_id = $playerId
  and player.role in ('default', 'hypercube', 'media')
order by pb.created_at desc
offset $offset limit $limit;

-- name: getBlocksBetween :many
select pb.player_id as blocker_id,
       player.*,
       pb.created_at as blocked_at
from player_blocks pb
         join player_data player on player.id = pb.target_id
where ((pb.player_id = $playerId and pb.target_id = $targetId)
    or ($bidirectional::bool and pb.player_id = $targetId and pb.target_id = $playerId))
  and player.role in ('default', 'hypercube', 'media');
