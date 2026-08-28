create table if not exists head_db
(
    id       int primary key,
    category varchar   not null,
    name     varchar   not null,
    tags     varchar[] not null,
    texture  varchar   not null
);
