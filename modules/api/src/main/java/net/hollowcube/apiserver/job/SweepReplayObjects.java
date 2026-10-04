package net.hollowcube.apiserver.job;

/// Asks for one shard of the replay bucket to be swept of objects no replay points at, as the data
/// of a [JobSpec#SWEEP_REPLAY_OBJECTS] row. The whole bucket, by hand:
///
/// ```sql
/// insert into jobs (job, instance, data)
/// select 'sweep-replay-objects', shard, json_build_object('shard', shard)
/// from (select lpad(to_hex(n), 4, '0') as shard from generate_series(0, 65535) as n) as shards
/// on conflict do nothing;
/// ```
///
/// @param shard leading hex digits of the hashed replay id
public record SweepReplayObjects(String shard) {}
