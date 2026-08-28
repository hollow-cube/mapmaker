-- name: listWidgets :many
select widget.*
from widget
order by id
limit $limit;

-- name: searchWidgets :many
-- not-null: total_count
select widget.*,
       count(*) over () as total_count
from widget
where name ilike $query
limit $limit offset $offset;
