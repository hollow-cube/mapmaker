package net.hollowcube.ipc.player;

import java.time.Instant;
import java.util.UUID;

public record Block(UUID blockerId, PlayerStub target, Instant blockedAt) {}
