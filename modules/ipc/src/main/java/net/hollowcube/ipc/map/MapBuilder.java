package net.hollowcube.ipc.map;

import net.hollowcube.common.util.RuntimeGson;

import java.time.Instant;
import java.util.UUID;

@RuntimeGson
public record MapBuilder(UUID id, Instant createdAt, boolean pending) {}
