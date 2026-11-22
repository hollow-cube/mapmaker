set local lock_timeout = '5s';

--> statement-breakpoint
alter table map_tags
    rename column index to position;

--> statement-breakpoint
alter table map_slots
    rename column index to position;

--> statement-breakpoint
alter index idx_map_slots_player_index_unique rename to idx_map_slots_player_position_unique;

--> statement-breakpoint
alter table map_reports
    rename column "time" to created_at;

--> statement-breakpoint
-- Ratings from before this have no time, so the default only applies to new rows.
alter table map_ratings
    add column rated_at timestamptz;

--> statement-breakpoint
alter table map_ratings
    alter column rated_at set default now();

--> statement-breakpoint
alter table map_features
    add constraint map_features_map_id_fkey foreign key (map_id) references maps (id) on delete cascade;

--> statement-breakpoint
alter table player_data
    alter column beta_enabled set not null;

--> statement-breakpoint
alter table player_sessions
    add constraint player_sessions_player_id_fkey foreign key (player_id) references player_data (id) on delete cascade;
