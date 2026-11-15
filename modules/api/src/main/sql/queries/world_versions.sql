-- name: claimEditingWorld :exec
-- Fails on `map_worlds_edit_map_idx` when another world holds the map for editing. The tracker may
-- already have inserted this world from its "created" message, untyped, so this takes that row over.
insert into map_worlds (id, map_id, server_id, type)
values ($worldId, $mapId, $serverId, 'editing')
on conflict (id) do update set type = 'editing';

-- name: getEditingWorld :one
select map_worlds.*
from map_worlds
where map_id = $mapId
  and type = 'editing';

-- name: releaseWorld :exec
delete
from map_worlds
where id = $worldId;

-- name: releaseEditingWorld :exec
delete
from map_worlds
where map_id = $mapId
  and type = 'editing';

-- name: getHead :one
select map_world_versions.*
from map_world_versions
where map_id = $mapId
order by id desc
limit 1;

-- name: insertVersion :exec
insert into map_world_versions (map_id, sha256, length, wal_seq, world_id)
values ($mapId, $sha256, $length, $walSeq, $worldId);
