alter table player_sessions
    add column if not exists reply_target uuid default null;
