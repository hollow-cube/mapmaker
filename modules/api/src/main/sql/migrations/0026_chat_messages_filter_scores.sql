alter table chat_messages add column if not exists filter_version varchar(64) default null;
alter table chat_messages add column if not exists filter_scores real[] default null;
