package net.hollowcube.mapmaker.hub.legacy;

import com.google.gson.JsonObject;
import net.hollowcube.mapmaker.model.MapData;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public record LegacyMap(@NotNull JsonObject raw) {

    public @NotNull String id() {
        return raw.get("id").getAsString();
    }

    public @NotNull String creatorUuid() {
        return raw.get("uuid").getAsString();
    }

    public @NotNull String name() {
        return raw.get("name").getAsString();
    }

    /** Convert to a modern {@link MapData}. */
    public @NotNull MapData toMapData() {
        var map = new MapData();
        map.setId(UUID.randomUUID().toString());
        map.setOwner(creatorUuid());
        map.setName(name());

        return map;
    }
}
