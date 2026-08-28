-- name: getRandomHeads :many
-- not-null: total_count
select head_db.*,
       count(*) over () as total_count
from head_db
order by random()
limit $limit;

-- name: getHeadsWithSearch :many
-- not-null: total_count
select head_db.*,
       count(*) over () as total_count
from head_db
where head_db_search(name, tags) @@ to_tsquery('simple', $query)
order by ts_rank(head_db_search(name, tags), to_tsquery('simple', $query)) desc, head_db.id
limit $limit offset $offset;

-- name: getHeadsWithCategory :many
-- not-null: total_count
select head_db.*,
       count(*) over () as total_count
from head_db
where category = $category
order by head_db.name, head_db.id
limit $limit offset $offset;
