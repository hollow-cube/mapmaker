-- name: findHub :one
-- Ready as of the Go tracker's last pod sync, which runs every 5s.
select id, cluster_ip, protocol_version
from server_states
where role = 'hub'
  and status = 1
  and ($exclude::text is null or id != $exclude)
limit 1;
