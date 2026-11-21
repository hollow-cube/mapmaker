-- name: insertBatch :exec
insert into map_wal (map_id, first_seq, last_seq, world_id, first_at, last_at, length, data, object_sha256)
values ($mapId, $firstSeq, $lastSeq, $worldId, $firstAt, $lastAt, $length, $data, $objectSha256)
on conflict (map_id, first_seq) do nothing;

-- name: getHeadSeq :one
-- not-null: head_seq
select coalesce(max(last_seq), 0)::bigint as head_seq
from map_wal
where map_id = $mapId;

-- name: getLength :one
-- not-null: bytes
-- Each batch is sent behind a 4 byte length.
select coalesce(sum(length::bigint + 4), 0)::bigint as bytes
from map_wal
where map_id = $mapId
  and first_seq > $afterSeq
  and last_seq <= $throughSeq;

-- name: listBatches :many
select first_seq, last_seq, length, data, object_sha256
from map_wal
where map_id = $mapId
  and first_seq > $afterSeq
  and last_seq <= $throughSeq
order by first_seq
limit $count;
