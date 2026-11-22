--> no-transaction
-- A plain `set not null` scans all of save_states under an exclusive lock, holding up every save
-- for the length of the scan. Validating a check first scans without blocking writes, and lets
-- `set not null` skip its own scan. Every statement is safe to run again.

set lock_timeout = '5s';

--> statement-breakpoint
alter table save_states
    drop constraint if exists save_states_protocol_version_present;

--> statement-breakpoint
alter table save_states
    add constraint save_states_protocol_version_present check (protocol_version is not null) not valid;

--> statement-breakpoint
alter table save_states validate constraint save_states_protocol_version_present;

--> statement-breakpoint
alter table save_states
    alter column protocol_version set not null;

--> statement-breakpoint
alter table save_states
    drop constraint save_states_protocol_version_present;
