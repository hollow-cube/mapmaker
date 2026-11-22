set local lock_timeout = '5s';

--> statement-breakpoint
alter table map_reports
    alter column id drop default;

--> statement-breakpoint
drop sequence map_reports_id_seq;

--> statement-breakpoint
alter table map_reports
    alter column id type bigint;

--> statement-breakpoint
alter table map_reports
    alter column id add generated always as identity;

--> statement-breakpoint
select setval(pg_get_serial_sequence('map_reports', 'id'), coalesce((select max(id) from map_reports), 0) + 1, false);

--> statement-breakpoint
alter table tebex_events
    alter column id drop default;

--> statement-breakpoint
drop sequence tebex_events_id_seq;

--> statement-breakpoint
alter table tebex_events
    alter column id type bigint;

--> statement-breakpoint
alter table tebex_events
    alter column id add generated always as identity;

--> statement-breakpoint
select setval(pg_get_serial_sequence('tebex_events', 'id'), coalesce((select max(id) from tebex_events), 0) + 1, false);

--> statement-breakpoint
alter table tx_log
    alter column id drop default;

--> statement-breakpoint
drop sequence tx_log_id_seq;

--> statement-breakpoint
alter table tx_log
    alter column id type bigint;

--> statement-breakpoint
alter table tx_log
    alter column id add generated always as identity;

--> statement-breakpoint
select setval(pg_get_serial_sequence('tx_log', 'id'), coalesce((select max(id) from tx_log), 0) + 1, false);

--> statement-breakpoint
-- Go reads punishments.id, so changing its type would fail Go's cached statements once per
-- connection; it stays integer.
alter table punishments
    alter column id drop default;

--> statement-breakpoint
drop sequence punishments_id_seq;

--> statement-breakpoint
alter table punishments
    alter column id add generated always as identity;

--> statement-breakpoint
select setval(pg_get_serial_sequence('punishments', 'id'), coalesce((select max(id) from punishments), 0) + 1, false);

--> statement-breakpoint
alter table player_sessions
    alter column last_seen set not null;

--> statement-breakpoint
alter table player_totp
    alter column created_at set not null,
    alter column active set not null;

--> statement-breakpoint
alter table api_keys
    alter column created_at set not null;
