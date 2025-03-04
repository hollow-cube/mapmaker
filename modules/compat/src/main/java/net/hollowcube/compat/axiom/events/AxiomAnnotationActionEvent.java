package net.hollowcube.compat.axiom.events;

import net.hollowcube.compat.axiom.data.annotations.actions.AnnotationAction;
import net.hollowcube.compat.axiom.data.buffers.AxiomBuffer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Called axiom performs an annotation action. ie. create, delete, move, clear, rotate.
 * <p>
 * When a module handles this event, it should set the {@link #handled} flag to true to prevent other modules from handling it.
 */
public final class AxiomAnnotationActionEvent implements AxiomEvent {

    private final @NotNull Player player;

    private final @NotNull List<AnnotationAction> actions;

    private boolean handled;

    public AxiomAnnotationActionEvent(
            @NotNull Player player,
            @NotNull List<AnnotationAction> actions
    ) {
        this.player = player;
        this.actions = actions;
    }

    @Override
    public @NotNull Player player() {
        return player;
    }

    public @NotNull List<AnnotationAction> actions() {
        return actions;
    }

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

}
