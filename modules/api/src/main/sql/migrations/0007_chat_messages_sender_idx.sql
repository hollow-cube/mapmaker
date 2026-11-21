create index if not exists chat_messages_sender_idx on chat_messages (sender, timestamp desc);
