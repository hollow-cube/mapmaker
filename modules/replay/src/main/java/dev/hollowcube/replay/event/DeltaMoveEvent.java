package dev.hollowcube.replay.event;

import net.minestom.server.coordinate.Pos;

public record DeltaMoveEvent(int entityId, Pos delta) implements ReplayEvent {
}
