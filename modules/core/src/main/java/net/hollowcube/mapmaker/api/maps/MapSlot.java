package net.hollowcube.mapmaker.api.maps;

import net.hollowcube.mapmaker.map.MapData;

import java.time.Instant;

public record MapSlot(MapData map, Instant createdAt) {
}
