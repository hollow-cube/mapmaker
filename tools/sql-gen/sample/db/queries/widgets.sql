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

-- name: listWidgetsAbove :many
-- nullable: $minimum
select widget.*
from widget
where $minimum::int is null
   or id > $minimum
order by id;
