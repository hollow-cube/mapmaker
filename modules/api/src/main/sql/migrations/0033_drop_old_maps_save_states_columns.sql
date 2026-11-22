--> no-transaction
-- Contract step: the columns 0031 replaced, and the dead ones beside them, go, along with the smaller
-- cleanups the schema review after it found (never-null columns, unused indexes, varchar -> text,
-- command_log ids). Outside a transaction so the save_states scan below does not run under the
-- exclusive lock the drops take, and so indexes can be dropped and built concurrently; every
-- statement here is safe to run again.

set lock_timeout = '5s';

--> statement-breakpoint
-- Only a document jsonb refused is still missing here, and that fails loudly rather than being
-- dropped with `state_v2`.
update save_states
set state = convert_from(state_v2, 'UTF8')::jsonb
where state is null;

--> statement-breakpoint
alter table save_states
    drop constraint if exists save_states_state_present;

--> statement-breakpoint
alter table save_states
    add constraint save_states_state_present check (state is not null) not valid;

--> statement-breakpoint
alter table save_states validate constraint save_states_state_present;

--> statement-breakpoint
alter table save_states
    alter column state set not null;

--> statement-breakpoint
alter table save_states
    drop constraint save_states_state_present;

--> statement-breakpoint
alter table save_states
    drop column if exists created,
    drop column if exists updated,
    drop column if exists deleted,
    drop column if exists state_v2;

--> statement-breakpoint
alter index if exists idx_save_states_live_map_player rename to idx_save_states_map_player;

--> statement-breakpoint
alter index if exists idx_save_states_live_completed_runs rename to idx_save_states_completed_runs;

--> statement-breakpoint
alter table maps
    drop column if exists m_type,
    drop column if exists authz_key,
    drop column if exists file_id,
    drop column if exists legacy_map_id,
    drop column if exists opt_name,
    drop column if exists opt_icon,
    drop column if exists size,
    drop column if exists opt_variant,
    drop column if exists opt_subvariant,
    drop column if exists opt_spawn_point,
    drop column if exists opt_extra,
    drop column if exists opt_tags,
    drop column if exists verification,
    drop column if exists quality_override;

--> statement-breakpoint
alter table map_stats
    drop column if exists clear_rate;

--> statement-breakpoint
alter table map_ratings
    drop column if exists comment;

--> statement-breakpoint
-- Every logged map_id and instance_id is a uuid, and every map_id and player_id names a row that
-- exists. instance_id points at map_worlds rows, which go when a world closes, and server_id at
-- server_states rows, which go with the pod, so neither can be a foreign key.
alter table command_log
    alter column map_id type uuid using map_id::uuid,
    alter column instance_id type uuid using instance_id::uuid;

--> statement-breakpoint
alter table command_log
    drop constraint if exists command_log_player_id_fkey,
    drop constraint if exists command_log_map_id_fkey;

--> statement-breakpoint
alter table command_log
    add constraint command_log_player_id_fkey foreign key (player_id) references player_data (id) not valid,
    add constraint command_log_map_id_fkey foreign key (map_id) references maps (id) on delete set null not valid;

--> statement-breakpoint
alter table command_log validate constraint command_log_player_id_fkey;

--> statement-breakpoint
alter table command_log validate constraint command_log_map_id_fkey;

--> statement-breakpoint
-- Never null in prod; the gameplay booleans stay until they move into the spawn checkpoint.
update maps
set extra = '{}'
where extra is null;

--> statement-breakpoint
alter table maps
    alter column extra set default '{}',
    alter column extra set not null,
    alter column protocol_version set not null,
    alter column opt_only_sprint set not null,
    alter column opt_no_sprint set not null,
    alter column opt_no_jump set not null,
    alter column opt_no_sneak set not null,
    alter column opt_boat set not null;

--> statement-breakpoint
drop index concurrently if exists idx_maps_leaderboard_format;

--> statement-breakpoint
drop index concurrently if exists player_notifications_key_idx;

--> statement-breakpoint
drop index concurrently if exists idx_map_slots_player_id;

--> statement-breakpoint
-- varchar -> text. None of these lengths was a constraint anything relied on, and the change is
-- binary compatible, so no table is rewritten.
alter table chat_messages
    alter column server_id type text,
    alter column channel type text,
    alter column sender type text,
    alter column target type text,
    alter column target drop default,
    alter column censored_by type text,
    alter column censored_by drop default,
    alter column filter_version type text,
    alter column filter_version drop default;

--> statement-breakpoint
alter table linked_accounts
    alter column social_id type text,
    alter column type type text;

--> statement-breakpoint
alter table map_reports
    alter column comment type text;

--> statement-breakpoint
alter table maps
    alter column deleted_reason type text;

--> statement-breakpoint
alter table pending_tebex_transactions
    alter column player_id type text,
    alter column username type text,
    alter column checkout_id type text,
    alter column basket_id type text;

--> statement-breakpoint
alter table pending_verification
    alter column type type text,
    alter column user_id type text,
    alter column user_secret type text;

--> statement-breakpoint
alter table player_cosmetics
    alter column cosmetic_path type text;

--> statement-breakpoint
alter table player_data
    alter column username type text;

--> statement-breakpoint
alter table player_notifications
    alter column type type text,
    alter column key type text;

--> statement-breakpoint
alter table player_sessions
    alter column username type text,
    alter column username drop default;

--> statement-breakpoint
alter table punishments
    alter column player_id type text,
    alter column executor_id type text,
    alter column type type text,
    alter column ladder_id type text,
    alter column comment type text,
    alter column revoked_by type text,
    alter column revoked_by drop default,
    alter column revoked_reason type text;

--> statement-breakpoint
alter table server_states
    alter column status_v2 type text,
    alter column status_v2 set default 'starting';

--> statement-breakpoint
alter table tebex_events
    alter column event_id type text;

--> statement-breakpoint
alter table tebex_state
    alter column tx_id type text;

--> statement-breakpoint
alter table tf_schematics
    alter column name type text,
    alter column filetype type text,
    alter column filetype set default 'unknown';

--> statement-breakpoint
alter table tx_log
    alter column reason type text,
    alter column currency type text,
    alter column currency set default 'cubits';

--> statement-breakpoint
-- head_db's search index is on a function of its varchar columns, so it is rebuilt around text ones.
drop index concurrently if exists head_db_search_idx;

--> statement-breakpoint
alter table head_db
    alter column category type text,
    alter column name type text,
    alter column tags type text[],
    alter column texture type text;

--> statement-breakpoint
drop function if exists head_db_search(varchar, varchar[]);

--> statement-breakpoint
create or replace function head_db_search(name text, tags text[]) returns tsvector
    language sql immutable strict parallel safe
as
$$
select to_tsvector('simple', name || ' ' || array_to_string(tags, ' '))
$$;

--> statement-breakpoint
create index concurrently if not exists head_db_search_idx on head_db using gin (head_db_search(name, tags));
