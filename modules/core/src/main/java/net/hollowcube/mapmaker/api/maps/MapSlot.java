package net.hollowcube.mapmaker.api.maps;

import net.hollowcube.mapmaker.map.MapData;
import org.jetbrains.annotations.UnknownNullability;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record MapSlot(
    MapData map,
    Instant createdAt,
    // Only present for published maps
    @UnknownNullability List<MapBuilder> builders
) {
    public MapSlot withLocalBuilder(String id) {
        var newBuilders = new ArrayList<>(builders);
        newBuilders.add(new MapBuilder(id, Instant.now(), true));
        return new MapSlot(map, createdAt, List.copyOf(newBuilders));
    }

    public MapSlot withoutLocalBuilder(String builderId) {
        var newBuilders = new ArrayList<>(builders);
        newBuilders.removeIf(builder -> builder.id().equals(builderId));
        return new MapSlot(map, createdAt, List.copyOf(newBuilders));
    }
}
