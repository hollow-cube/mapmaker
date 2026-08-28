create table widget
(
    id     int primary key,
    name   varchar   not null,
    labels varchar[] not null,
    note   varchar
);
