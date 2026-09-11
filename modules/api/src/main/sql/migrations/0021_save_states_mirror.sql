-- Describe-time mirror of existing Go indexes (000013, 000014, 000030, 000032). Nothing to apply.
create index if not exists idx_save_states_map_player on save_states (map_id, player_id) where deleted is null;
create index if not exists idx_save_states_map_id on save_states (map_id);
create index if not exists idx_maps_leaderboard_format on maps ((leaderboard ->> 'format'));
create index if not exists idx_save_states_completed_runs on save_states (map_id)
    where deleted is null and completed = true and playtime != 0 and (type = 'playing' or type = 'verifying');
