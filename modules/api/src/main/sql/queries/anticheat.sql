-- name: upsertAnticheatTrace :exec
-- Filed after the blob is in place. Keyed on the id the proxy generated, so a ship that retried
-- after the row was already written replaces it rather than failing: the file it just wrote is the
-- same file, at the same path, and its header may only have grown more complete.
-- `pinned` and `created_at` are the two columns that are not the header's, so they survive.
insert into anticheat_traces (id, capture_id, player_id, proxy_version, proxy, client_pvn, reason,
                                format_version, started_at, ended_at, bytes, path)
values ($id, $captureId, $playerId, $proxyVersion, $proxy, $clientPvn, $reason,
        $formatVersion, $startedAt, $endedAt, $bytes, $path)
on conflict (id) do update
    set capture_id     = excluded.capture_id,
        player_id      = excluded.player_id,
        proxy_version  = excluded.proxy_version,
        proxy          = excluded.proxy,
        client_pvn      = excluded.client_pvn,
        reason         = excluded.reason,
        format_version = excluded.format_version,
        started_at     = excluded.started_at,
        ended_at       = excluded.ended_at,
        bytes          = excluded.bytes,
        path           = excluded.path;

-- name: getAnticheatTrace :one
select anticheat_traces.*
from anticheat_traces
where id = $id;

-- name: listAnticheatTracesByCapture :many
-- One capture is a handful of traces — a server switch or a ring flush each start a new one — so
-- this is unpaged on purpose, and ordered the way they were recorded.
select anticheat_traces.*
from anticheat_traces
where capture_id = $captureId
order by started_at, id;

-- name: deleteExpiredAnticheatTraces :many
-- For the sweeper there is no retention policy for yet: nothing sets expires_at today. Returns the
-- rows so the caller can unlink the blobs the paths point at, which is the only reason to run it.
delete
from anticheat_traces
where expires_at is not null
  and expires_at <= now()
  and not pinned
returning anticheat_traces.*;
