package net.hollowcube.compat.axiom.events;

import net.hollowcube.compat.axiom.data.buffers.AxiomBuffer;
import net.minestom.server.entity.Player;

import java.util.UUID;

/**
 * Called axiom performs a buffer operation on the world, ie. changing blocks or biomes.
 * <p>
 * When a module handles this event, it should set the {@link #handled} flag to true to prevent other modules from handling it.
 */
public final class AxiomApplyBufferEvent implements AxiomEvent {

    private final Player player;

    private final UUID id;
    private final AxiomBuffer buffer;

    private boolean handled;

    public AxiomApplyBufferEvent(Player player, UUID id, AxiomBuffer buffer) {
        this.player = player;
        this.id = id;
        this.buffer = buffer;
    }

    @Override
    public Player player() {
        return player;
    }

    public UUID id() {
        return id;
    }

    public AxiomBuffer buffer() {
        return buffer;
    }

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

}
