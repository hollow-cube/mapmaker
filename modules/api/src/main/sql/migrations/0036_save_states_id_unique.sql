--> no-transaction
-- The index the primary key moves onto from (id, map_id, player_id). Building it fails on a
-- duplicated id rather than skipping it; a failed concurrent build leaves an invalid index behind,
-- which is why a rerun drops it first.

drop index concurrently if exists save_states_id_key;

--> statement-breakpoint
create unique index concurrently save_states_id_key on save_states (id);
