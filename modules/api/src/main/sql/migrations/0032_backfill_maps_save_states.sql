--> no-transaction
-- Backfills the maps columns 0031 added and makes the new columns what reads can rely on. Outside a
-- transaction so the indexes can be built concurrently; every statement here is safe to run again.
--
-- save_states is too large to backfill here without holding every row's lock to the end; it is
-- backfilled beforehand with tools/backfill/save_states_0031.sh, and this refuses to continue
-- until that is done.

-- The schema changes below each take a brief exclusive lock; waiting on one behind a long query
-- would stall every save queued after it, so give up instead and let the deploy be retried.
set lock_timeout = '5s';

--> statement-breakpoint
update maps
set name               = coalesce(opt_name, ''),
    icon               = coalesce(opt_icon, ''),
    variant            = coalesce(case when opt_variant = any (enum_range(null::map_variant)::text[]) then opt_variant::map_variant end, 'parkour'),
    subvariant         = case when opt_subvariant = any (enum_range(null::map_subvariant)::text[]) then opt_subvariant::map_subvariant end,
    spawn_point        = opt_spawn_point,
    extra              = convert_from(opt_extra, 'UTF8')::jsonb,
    size_class         = coalesce((enum_range(null::map_size))[(size + 1)::int], 'normal'),
    verification_state = coalesce((enum_range(null::map_verification))[(coalesce(verification, 0) + 1)::int], 'unverified'),
    quality            = coalesce((enum_range(null::map_quality))[(coalesce(quality_override, 0) + 1)::int], 'unrated')
where name is null
   or icon is null
   or variant is null
   or spawn_point is null
   or size_class is null
   or verification_state is null
   or quality is null;

--> statement-breakpoint
alter table maps
    alter column name set not null,
    alter column icon set not null,
    alter column variant set not null,
    alter column spawn_point set not null,
    alter column size_class set not null,
    alter column verification_state set not null,
    alter column quality set not null;

--> statement-breakpoint
-- Fails on the first row the backfill missed. The cast is of a value from the row because a constant
-- one would be folded, and fail, while planning.
select ('save_states is not backfilled, run tools/backfill/save_states_0031.sh: ' || id)::int
from save_states
where state is null
   or created_at is distinct from created
   or updated_at is distinct from updated
   or deleted_at is distinct from deleted
limit 1;

--> statement-breakpoint
-- `state` stays nullable until the old columns go: the build this replaces still writes null for
-- a document jsonb refuses, and would fail the save instead.
alter table save_states
    drop constraint if exists save_states_created_at_not_null,
    drop constraint if exists save_states_updated_at_not_null;

--> statement-breakpoint
alter table save_states
    add constraint save_states_created_at_not_null check (created_at is not null) not valid,
    add constraint save_states_updated_at_not_null check (updated_at is not null) not valid;

--> statement-breakpoint
alter table save_states validate constraint save_states_created_at_not_null;

--> statement-breakpoint
alter table save_states validate constraint save_states_updated_at_not_null;

--> statement-breakpoint
-- The validated checks let these skip the table scan.
alter table save_states
    alter column created_at set not null,
    alter column updated_at set not null;

--> statement-breakpoint
alter table save_states
    drop constraint save_states_created_at_not_null,
    drop constraint save_states_updated_at_not_null;

--> statement-breakpoint
reset lock_timeout;

--> statement-breakpoint
drop index concurrently if exists idx_save_states_live_map_player;

--> statement-breakpoint
create index concurrently idx_save_states_live_map_player on save_states (map_id, player_id)
    where deleted_at is null;

--> statement-breakpoint
drop index concurrently if exists idx_save_states_live_completed_runs;

--> statement-breakpoint
create index concurrently idx_save_states_live_completed_runs on save_states (map_id)
    where deleted_at is null and completed and playtime <> 0 and type in ('playing', 'verifying');

--> statement-breakpoint
drop index concurrently if exists idx_save_states_updated_at;

--> statement-breakpoint
create index concurrently idx_save_states_updated_at on save_states (updated_at);
