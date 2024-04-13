package net.hollowcube.mapmaker.map.ram.event;

import net.hollowcube.mapmaker.map.world.EditingMapWorld;
import net.minestom.server.entity.Player;
import net.minestom.server.event.Event;
import org.jetbrains.annotations.NotNull;

public record AddRamObjectEvent(
        @NotNull Player player,
        @NotNull EditingMapWorld world
) implements Event {
}
