-- name: insertBatch :exec
insert into map_wal (map_id, first_seq, last_seq, world_id, first_at, last_at, length, data, object_sha256)
values ($mapId, $firstSeq, $lastSeq, $worldId, $firstAt, $lastAt, $length, $data, $objectSha256)
on conflict (map_id, first_seq) do nothing;

-- name: getHeadSeq :one
-- not-null: head_seq
select coalesce(max(last_seq), 0)::bigint as head_seq
from map_wal
where map_id = $mapId;
