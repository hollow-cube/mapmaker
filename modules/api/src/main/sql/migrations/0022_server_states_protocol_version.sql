-- Describe-time mirror of Go's 000006, 000007 and 000012, in the order they ran. Nothing to apply.
alter table server_states add column if not exists status_v2 varchar(10) default 'starting';
alter table server_states alter column status_v2 set not null;
alter table server_states add column if not exists status_since timestamptz not null default now();
-- 0 until the tracker has asked the server, and for a build too old to answer.
alter table server_states add column if not exists protocol_version int not null default 0;
