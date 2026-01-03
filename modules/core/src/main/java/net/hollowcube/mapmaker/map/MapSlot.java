package net.hollowcube.mapmaker.map;

import net.hollowcube.common.util.RuntimeGson;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@RuntimeGson
public record MapSlot(
    @NotNull MapData map,
    @NotNull Instant createdAt,
    int index
) {
}
