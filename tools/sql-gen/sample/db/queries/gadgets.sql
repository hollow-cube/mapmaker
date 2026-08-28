-- name: insertGadget :exec
insert into gadget (id, status, label, quantity, weight, price, payload, metadata, created_at,
                    released_on, tags, ranks)
values ($id, $status, $label, $quantity, $weight, $price, $payload, $metadata, $createdAt,
        $releasedOn, $tags, $ranks);

-- name: getGadget :one
select gadget.*
from gadget
where id = $id;

-- name: countGadgets :one
-- not-null: count
select count(*)
from gadget;

-- name: listGadgetLabels :many
select label
from gadget
order by label;

-- name: findGadgetsByText :many
select gadget.*
from gadget
where label = $needle
   or note = $needle
order by id;

-- name: listGadgetsWithStars :many
-- nullable: stars
select gadget.*, review.stars
from gadget
         left join gadget_review review on review.gadget_id = gadget.id
order by gadget.id;

-- name: listGadgetsWithReview :many
-- nullable: review
select gadget.*, review.*
from gadget
         left join gadget_review review on review.gadget_id = gadget.id
order by gadget.id;

-- name: pairGadgets :many
select a.*, b.*
from gadget a
         join gadget b on b.label = a.label and b.id > a.id
order by a.id, b.id;

-- name: insertReview :one
insert into gadget_review (id, gadget_id, stars)
values ($id, $gadgetId, $stars)
returning gadget_review.*;

-- name: deleteGadget :exec
delete
from gadget
where id = $id;

-- name: searchGadgets :many
-- not-null: total_count
-- not-null: label_prefix
select gadget.*,
       count(*) over ()                     as total_count,
       left(gadget.label, $prefixLength)    as label_prefix
from gadget
/* where */
/* order by */
limit $limit offset $offset;
