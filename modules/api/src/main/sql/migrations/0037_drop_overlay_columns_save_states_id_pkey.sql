set local lock_timeout = '5s';

--> statement-breakpoint
alter table maps
    drop column ext;

--> statement-breakpoint
alter table player_sessions
    drop column username;

--> statement-breakpoint
-- 0036 built save_states_id_key; taking it as the primary key needs no scan, and the index takes
-- the constraint's name.
alter table save_states
    drop constraint save_states_pkey,
    add constraint save_states_pkey primary key using index save_states_id_key;
