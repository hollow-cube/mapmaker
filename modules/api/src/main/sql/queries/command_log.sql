-- name: insertCommandLog :exec
-- One row per command, written as it is run.
insert into command_log (timestamp, player_id, server_id, map_id, instance_id, command, remote, outcome, error, duration_ms)
values ($timestamp, $playerId, $serverId, $mapId, $instanceId, $command, $remote, $outcome, $error, $durationMs);
