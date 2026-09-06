package net.hollowcube.ipc.map;

import java.time.Instant;
import java.util.List;

public record MapSlot(MapData map, Instant createdAt, boolean owner, List<MapBuilder> builders) {}
