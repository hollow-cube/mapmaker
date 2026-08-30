-- Ours, not Go's: apply by hand.
--
-- The table has never had an index, and every question anyone asks of it is "what did this player
-- say", newest first.
create index if not exists chat_messages_sender_idx on chat_messages (sender, timestamp desc);
