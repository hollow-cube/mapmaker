package net.hollowcube.compat.axiom.events;

import net.hollowcube.compat.axiom.data.buffers.AxiomBuffer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

/**
 * Called axiom performs a buffer operation on the world, ie. changing blocks or biomes.
 * <p>
 * When a module handles this event, it should set the {@link #handled} flag to true to prevent other modules from handling it.
 */
public final class AxiomApplyBufferEvent implements AxiomEvent {

    private final @NotNull Player player;

    private final @NotNull UUID id;
    private final @NotNull AxiomBuffer buffer;

    private boolean handled;

    public AxiomApplyBufferEvent(
            @NotNull Player player,
            @NotNull UUID id,
            @NotNull AxiomBuffer buffer
    ) {
        this.player = player;
        this.id = id;
        this.buffer = buffer;
    }

    @Override
    public @NotNull Player player() {
        return player;
    }

    public @NotNull UUID id() {
        return id;
    }

    public @NotNull AxiomBuffer buffer() {
        return buffer;
    }

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

}
