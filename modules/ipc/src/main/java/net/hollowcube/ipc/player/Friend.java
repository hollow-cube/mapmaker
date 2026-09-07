package net.hollowcube.ipc.player;

import java.time.Instant;

public record Friend(PlayerStub player, boolean online, Instant lastOnline, Instant friendsSince) {}
