-- name: countPlayerSessions :one
-- not-null: count
select count(*) as count
from player_sessions;
