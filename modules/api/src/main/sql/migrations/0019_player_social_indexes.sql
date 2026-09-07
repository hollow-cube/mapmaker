-- Additional indexes beyond Go's schema; apply by hand with CREATE INDEX CONCURRENTLY.
create index if not exists ip_history_address_idx on ip_history (address);

create index if not exists player_friend_requests_target_idx on player_friend_requests (target_id);

create index if not exists player_notifications_player_type_key_idx
    on player_notifications (player_id, type, key);

create index if not exists player_notifications_player_created_idx
    on player_notifications (player_id, created_at desc)
    where deleted_at is null;
