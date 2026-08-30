-- name: insertChatMessage :exec
-- Every message, censored or not: a message nobody saw is the one a moderator most wants to read.
-- `channel` keeps Go's convention — 'global'/'local'/'staff', or the target's uuid for a direct
-- message — so a row written here and a row written by Go read alike. `target` is Go's "unused,
-- should delete" column and stays unwritten.
insert into chat_messages (timestamp, server_id, channel, sender, content, censored_by, censored_detail)
values (now(), $serverId, $channel, $sender, $content, $censoredBy, $censoredDetail);

-- name: getReplyTarget :one
-- Null for a player with nobody to reply to and for one with no session at all, which want the
-- same answer.
select reply_target
from player_sessions
where player_id = $playerId;

-- name: setReplyTargets :exec
-- Both sides of a direct message in one statement: answering someone makes you the person they
-- reply to, so each of the pair points at the other.
update player_sessions
set reply_target = case player_id when $sender::uuid then $target::uuid else $sender::uuid end
where player_id in ($sender::uuid, $target::uuid);

-- name: playerOnline :one
-- not-null: online
-- Anywhere in the network, which is what having a session row means.
select count(*) > 0 as online
from player_sessions
where player_id = $playerId;
