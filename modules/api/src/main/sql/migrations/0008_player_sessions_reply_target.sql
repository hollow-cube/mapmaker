-- Ours, not Go's: apply by hand, before the api-server that writes it is deployed.
--
-- Who a player is replying to when they type `/r`, which used to be a redis key with no owner and
-- an explicit delete in the player manager to keep it from outliving the session. On the session
-- row it has exactly the lifetime it should: Go's sqlc names its columns, so its upsert on transfer
-- leaves the target alone and its delete drops it with the session.
alter table player_sessions
    add column if not exists reply_target uuid default null;
