package net.hollowcube.compat.lunar.events;

import net.hollowcube.compat.lunar.payload.InstalledModsResponsePayload;
import net.minestom.server.entity.Player;

public record LunarPlayerInitEvent(
    Player player,
    InstalledModsResponsePayload payload
) implements LunarPlayerEvent {

}
