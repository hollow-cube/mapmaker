package net.hollowcube.ipc.player;

import java.time.Instant;

public record FriendRequest(PlayerStub player, Instant sentAt) {}
