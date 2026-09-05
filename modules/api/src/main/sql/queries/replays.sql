-- name: getReplay :one
select replays.*
from replays
where id = $id;

-- name: getReplayForUpdate :one
-- The lock every write takes first. Preconditions are checked in Java against the locked row rather
-- than as a `where version =` guard, which is what Go does.
select replays.*
from replays
where id = $id
    for update;

-- name: createReplayIfAbsent :one
-- Null when someone else won the insert; the caller re-locks and evaluates that row instead.
insert into replays (id, version, recording_revision, state, representation, next_segment_index,
                     current_preamble, current_preamble_digest, outcome)
values ($id, 1, 1, $state, 'segmented', $nextSegmentIndex, $currentPreamble, $currentPreambleDigest, $outcome)
on conflict (id) do nothing
returning replays.*;

-- name: updateReplayRecording :one
-- `version` moves on every write; `recording_revision` moves only here, so a segment's
-- `commit_revision` names the commit that inserted it.
update replays
set version                 = version + 1,
    recording_revision      = recording_revision + 1,
    state                   = $state,
    next_segment_index      = $nextSegmentIndex,
    current_preamble        = $currentPreamble,
    current_preamble_digest = $currentPreambleDigest,
    outcome                 = $outcome,
    updated_at              = now()
where id = $id
returning replays.*;

-- name: publishReplayCompacted :one
-- The pointer swap. `state` and `outcome` are untouched: compaction does not change why a recording
-- ended.
update replays
set version                   = version + 1,
    representation            = 'compacted',
    current_preamble          = $currentPreamble,
    current_preamble_digest   = $currentPreambleDigest,
    compacted_source_revision = recording_revision,
    compacted_object          = $compactedObject,
    compacted_length          = $compactedLength,
    compacted_digest          = $compactedDigest,
    updated_at                = now()
where id = $id
returning replays.*;

-- name: createReplaySegment :exec
-- Exactly one of `objectReference` and `data` is non-null; a check constraint enforces it.
insert into replay_segments (replay_id, segment_index, object_reference, data, length, digest, commit_revision)
values ($replayId, $segmentIndex, $objectReference, $data, $length, $digest, $commitRevision);

-- name: getReplaySegment :one
select replay_segments.*
from replay_segments
where replay_id = $replayId
  and segment_index = $segmentIndex;

-- name: getReplayIdempotency :one
select replay_idempotency.*
from replay_idempotency
where replay_id = $replayId
  and idempotency_key = $idempotencyKey;

-- name: createReplayIdempotency :exec
insert into replay_idempotency (replay_id, idempotency_key, request_fingerprint, response_status,
                                response_etag, response_metadata)
values ($replayId, $idempotencyKey, $requestFingerprint, $responseStatus, $responseEtag, $responseMetadata);

-- name: listUncompactedReplays :many
-- The reconciler's backstop for a compaction row never enqueued, parked, or finished by the Go
-- server. `before` keeps it off a commit that is still in flight.
select id
from replays
where state = 'finished'
  and representation = 'segmented'
  and updated_at < $before
order by updated_at
limit $limit;

-- name: listCompactedReplaysWithSegments :many
-- Compacted replays whose sources are past the grace period.
select id
from replays
where representation = 'compacted'
  and updated_at < $before
  and exists (select 1 from replay_segments where replay_id = replays.id)
order by updated_at
limit $limit;

-- name: listReplaySegmentObjects :many
-- A null `object_reference` is an inline segment, with nothing to unlink.
select segment_index, object_reference
from replay_segments
where replay_id = $replayId
order by segment_index;

-- name: deleteReplaySegments :exec
-- After the objects are gone, never before: an interrupted sweep must leave rows pointing at
-- deleted objects rather than orphan the objects.
delete
from replay_segments
where replay_id = $replayId;

-- name: deleteExpiredReplayIdempotency :exec
-- A record only has to outlive the retries of the request it recorded, which is minutes.
delete
from replay_idempotency
where created_at < $before;
