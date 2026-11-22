#!/usr/bin/env bash
# Copies save_states into the columns migration 0031 added, 5000 rows per transaction, so no save
# waits on more than one batch. Run it once 0031 is applied and before deploying 0032, which refuses
# to run until it has finished. Safe to stop and rerun; each worker resumes from its cursor.
#
#   PSQL='psql postgres://postgres:postgres@localhost:5432/postgres' tools/backfill/save_states_0031.sh
#   PSQL='kubectl -n mapmaker exec -i postgres-v1-1 -c postgres -- psql -U postgres -d api-server' tools/backfill/save_states_0031.sh
set -euo pipefail

: "${PSQL:?set PSQL to the psql command for the target database}"

eval "psql=($PSQL)"
run() { "${psql[@]}" -v ON_ERROR_STOP=1 -Atq "$@"; }

# Four workers, each walking a quarter of the id space: the work is bound on reading the table, and
# one walker leaves the disk mostly idle.
bounds=(00000000 40000000 80000000 c0000000)

run -c "
create table if not exists save_states_backfill_cursor
(
    worker    int  not null primary key,
    id        uuid not null,
    map_id    uuid not null,
    player_id uuid not null,
    until     uuid
);
insert into save_states_backfill_cursor
values (0, '${bounds[0]}-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', '${bounds[1]}-0000-0000-0000-000000000000'),
       (1, '${bounds[1]}-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', '${bounds[2]}-0000-0000-0000-000000000000'),
       (2, '${bounds[2]}-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', '${bounds[3]}-0000-0000-0000-000000000000'),
       (3, '${bounds[3]}-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', '00000000-0000-0000-0000-000000000000', null)
on conflict (worker) do nothing;"

# Each round is one primary key range after the worker's cursor, then the cursor moves to its end.
# A save holding a row lock makes the round give up after 2s rather than queue behind it; the worker
# stops and a rerun resumes it.
batch() {
    echo "
set lock_timeout = '2s';
with pos as (select * from save_states_backfill_cursor where worker = $1),
     batch as (select s.id, s.map_id, s.player_id
               from save_states s, pos c
               where (s.id, s.map_id, s.player_id) > (c.id, c.map_id, c.player_id)
                 and (c.until is null or s.id < c.until)
               order by s.id, s.map_id, s.player_id
               limit 5000),
     filled as (
         update save_states s
             set created_at = s.created,
                 updated_at = s.updated,
                 deleted_at = s.deleted,
                 state = coalesce(s.state, convert_from(s.state_v2, 'UTF8')::jsonb)
             from batch b
             where (s.id, s.map_id, s.player_id) = (b.id, b.map_id, b.player_id)
                 and (s.state is null
                     or s.created_at is distinct from s.created
                     or s.updated_at is distinct from s.updated
                     or s.deleted_at is distinct from s.deleted))
update save_states_backfill_cursor c
set id = last.id, map_id = last.map_id, player_id = last.player_id
from (select * from batch order by id desc, map_id desc, player_id desc limit 1) last
where c.worker = $1
returning 1;"
}

work() {
    local rounds=0 statement
    statement=$(batch "$1")
    while [[ -n "$(run -c "$statement")" ]]; do
        rounds=$((rounds + 1))
        if ((rounds % 50 == 0)); then echo "worker $1: $rounds rounds ($((rounds * 5000)) rows) in $((SECONDS - started))s"; fi
    done
    echo "worker $1: done after $rounds rounds in $((SECONDS - started))s"
}

started=$SECONDS
pids=()
for worker in 0 1 2 3; do
    work "$worker" &
    pids+=($!)
done
failed=0
for pid in "${pids[@]}"; do wait "$pid" || failed=1; done
if ((failed)); then
    echo "a worker failed; rerun to resume" >&2
    exit 1
fi

run -c "drop table save_states_backfill_cursor;"
echo "rows still differing:"
run -c "
select count(*)
from save_states
where state is null
   or created_at is distinct from created
   or updated_at is distinct from updated
   or deleted_at is distinct from deleted;"
