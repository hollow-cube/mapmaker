-- A player's inbox. Deletion is soft (`deleted_at`), as Go's, except for the replace-on-write
-- path which deletes every existing row with the same type and key.

-- name: list :many
-- not-null: total_count
select player_notifications.*,
       count(*) over () as total_count
from player_notifications
where player_id = $playerId
  and deleted_at is null
  and (expires_at is null or expires_at > now())
  and (not $unreadOnly::bool or read_at is null)
order by created_at desc
offset $offset limit $limit;

-- name: markRead :exec
update player_notifications
set read_at = case when $read::bool then now() else null end
where id = $id
  and deleted_at is null;

-- name: delete :exec
update player_notifications
set deleted_at = now()
where id = $id
  and deleted_at is null;

-- name: insert :exec
insert into player_notifications (id, type, key, player_id, data, expires_at)
values ($id, $type, $key, $playerId, $data, $expiresAt);

-- name: replace :exec
-- Go's Unsafe_DeleteNotification, run before an insert that replaces what is there: read or not,
-- the row goes, so the player has one notification for the key rather than a history of them.
delete
from player_notifications
where type = $type
  and key = $key
  and player_id = $playerId;

-- name: deleteByKey :exec
-- What a friend request's cancel, decline or the block that swallowed it leaves behind.
update player_notifications
set deleted_at = now()
where type = $type
  and key = $key
  and player_id = $playerId
  and deleted_at is null;
