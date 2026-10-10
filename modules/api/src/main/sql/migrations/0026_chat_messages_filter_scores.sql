-- Ours, not Go's: apply by hand to the maps database before deploying the chat filter.
--
-- What the trained chat filter scored a message, while it runs in shadow beside the word list:
-- probabilities in the order its manifest lists, under the version that wrote them, so that two
-- versions' rows are never compared as one. Null for a message nothing scored.
alter table chat_messages add column if not exists filter_version varchar(64) default null;
alter table chat_messages add column if not exists filter_scores real[] default null;
