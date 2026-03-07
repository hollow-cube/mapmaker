package net.hollowcube.mapmaker.api.maps;

import net.hollowcube.mapmaker.map.MapData;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;
import java.util.List;

public record MapSlot(
    MapData map,
    Instant createdAt,
    // Only present for published maps
    @UnknownNullability List<MapBuilder> builders
) {
}
