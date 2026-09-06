package net.hollowcube.ipc.map;

import java.time.Instant;
import java.util.UUID;

public record MapBuilder(UUID id, Instant createdAt, boolean pending) {}
