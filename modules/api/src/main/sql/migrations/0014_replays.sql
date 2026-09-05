-- The Go api-server's replay storage, as its `internal/mapdb` migrations 000034 and 000035 leave
-- the tables. Go owns the schema; this is what the queries here are described against, and has to
-- be kept in step with it by hand. Nothing to apply.
--
-- A replay is a preamble plus a run of immutable segments while recording, and one compacted object
-- once it is done. `recording_revision` counts only the commits that appended, which is what a
-- segment's `commit_revision` points back at.
create table if not exists replays
(
    id                        text        primary key,
    version                   bigint      not null,
    recording_revision        bigint      not null,
    state                     text        not null,
    representation            text        not null,
    next_segment_index        bigint      not null,
    current_preamble          bytea       not null,
    current_preamble_digest   bytea       not null,
    compacted_source_revision bigint,
    compacted_object          text,
    compacted_length          bigint,
    compacted_digest          bytea,
    created_at                timestamptz not null default now(),
    updated_at                timestamptz not null default now(),

    constraint replay_id_not_empty check (id <> '' and length(id) <= 512),
    constraint replay_version_positive check (version > 0),
    constraint replay_recording_revision_positive check (recording_revision > 0),
    constraint replay_state_valid check (state in ('recording', 'finished')),
    constraint replay_representation_valid check (representation in ('segmented', 'compacted')),
    constraint replay_next_segment_index_nonnegative check (next_segment_index >= 0),
    constraint replay_preamble_digest_is_sha256 check (octet_length(current_preamble_digest) = 32),
    constraint replay_compacted_digest_is_sha256 check (
        compacted_digest is null or octet_length(compacted_digest) = 32
        ),
    constraint replay_compacted_length_nonnegative check (compacted_length is null or compacted_length >= 0),
    constraint replay_compacted_fields_consistent check (
        (
            representation = 'segmented'
                and compacted_source_revision is null
                and compacted_object is null
                and compacted_length is null
                and compacted_digest is null
            )
            or
        (
            representation = 'compacted'
                and state = 'finished'
                and compacted_source_revision is not null
                and compacted_object is not null
                and compacted_length is not null
                and compacted_digest is not null
            )
        )
);

-- `data` and `object_reference` are exactly-one: a segment under the inline threshold is a column
-- of this row, and anything larger is an object. The read path branches on which is populated and
-- never on the current threshold, so moving the threshold cannot misroute an older segment.
--
-- Written as Go's two migrations rather than as the table they add up to. `data` was added by the
-- second, and Postgres appends an added column: flattening this would put `data` in the middle here
-- and at the end in production, and `select replay_segments.*` is read by column position.
create table if not exists replay_segments
(
    replay_id        text   not null references replays (id) on delete cascade,
    segment_index    bigint not null,
    object_reference text   not null,
    length           bigint not null,
    digest           bytea  not null,
    commit_revision  bigint not null,

    primary key (replay_id, segment_index),
    unique (object_reference),
    constraint replay_segment_index_nonnegative check (segment_index >= 0),
    constraint replay_segment_length_nonnegative check (length >= 0),
    constraint replay_segment_digest_is_sha256 check (octet_length(digest) = 32),
    constraint replay_segment_commit_revision_positive check (commit_revision > 0)
);

alter table replay_segments
    add column if not exists data bytea;
alter table replay_segments
    alter column object_reference drop not null;
alter table replay_segments
    drop constraint if exists replay_segment_storage_exclusive;
alter table replay_segments
    add constraint replay_segment_storage_exclusive check ((data is null) <> (object_reference is null));

-- One row per successful write, so a recorder that lost the response can send the same request
-- again and be told what it was told the first time.
create table if not exists replay_idempotency
(
    replay_id           text        not null references replays (id) on delete cascade,
    idempotency_key     text        not null,
    request_fingerprint bytea       not null,
    response_status     smallint    not null,
    response_etag       text        not null,
    response_metadata   jsonb       not null,
    created_at          timestamptz not null default now(),

    primary key (replay_id, idempotency_key),
    constraint replay_idempotency_key_not_empty check (
        idempotency_key <> '' and length(idempotency_key) <= 512
        ),
    constraint replay_request_fingerprint_is_sha256 check (octet_length(request_fingerprint) = 32),
    constraint replay_response_status_valid check (response_status between 200 and 299)
);
