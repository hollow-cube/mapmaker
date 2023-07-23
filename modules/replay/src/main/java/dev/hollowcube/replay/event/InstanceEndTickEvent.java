package dev.hollowcube.replay.event;

import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

public record InstanceEndTickEvent(
        @NotNull Instance instance
) implements InstanceEvent {

    @Override
    public @NotNull Instance getInstance() {
        return instance;
    }

}
