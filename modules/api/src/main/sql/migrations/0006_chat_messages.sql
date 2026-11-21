create table if not exists chat_messages
(
    timestamp       timestamptz not null,
    server_id       varchar(50) not null,
    channel         varchar(50) not null,
    sender          varchar(36) not null,
    target          varchar(50) default null,
    content         text        not null,

    censored_by     varchar(36) default null,
    censored_detail text        default null
);
