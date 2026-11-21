alter table server_states add column if not exists status_v2 varchar(10) default 'starting';
alter table server_states alter column status_v2 set not null;
alter table server_states add column if not exists status_since timestamptz not null default now();
alter table server_states add column if not exists protocol_version int not null default 0;
