package net.hollowcube.ipc.map;

import net.hollowcube.common.util.RuntimeGson;

import java.time.Instant;
import java.util.List;

@RuntimeGson
public record MapSlot(MapData map, Instant createdAt, boolean owner, List<MapBuilder> builders) {}
