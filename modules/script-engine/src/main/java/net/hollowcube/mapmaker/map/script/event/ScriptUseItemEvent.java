package net.hollowcube.mapmaker.map.script.event;

import net.minestom.server.entity.Player;
import net.minestom.server.event.trait.PlayerInstanceEvent;
import org.jetbrains.annotations.NotNull;

public record ScriptUseItemEvent(
        @NotNull Player player
) implements PlayerInstanceEvent {

    @Override
    public @NotNull Player getPlayer() {
        return player;
    }

}
