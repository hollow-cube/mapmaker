package net.hollowcube.apiserver.job;

import org.jetbrains.annotations.Nullable;

/// `insert into jobs (job, instance, data) values ('sample-replay-transcode', 'sample', '{"count": 500}')`
///
/// @param after the replay id to start after; null starts at a random one
public record SampleReplayTranscode(int count, @Nullable String after) {}
