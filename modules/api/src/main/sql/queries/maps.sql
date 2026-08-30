-- name: isMapPublished :one
-- not-null: published
-- Whether `[map]` in a message is a map the people reading it could open. Matches what a map server
-- used to answer for itself out of `MapData.isPublished`.
select count(*) > 0 as published
from maps
where id = $mapId
  and published_id is not null;
