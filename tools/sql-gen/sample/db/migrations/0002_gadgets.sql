create type gadget_status as enum ('draft', 'active', 'retired');

create table gadget
(
    id          uuid          primary key,
    status      gadget_status not null,
    label       varchar       not null,
    quantity    int           not null,
    weight      float8        not null,
    price       numeric       not null,
    payload     bytea         not null,
    metadata    jsonb         not null,
    created_at  timestamptz   not null,
    released_on date          not null,
    tags        varchar[]     not null,
    ranks       int[]         not null,
    note        varchar,
    retired_at  timestamptz
);

create table gadget_review
(
    id        int  primary key,
    gadget_id uuid not null references gadget (id),
    stars     int  not null
);
