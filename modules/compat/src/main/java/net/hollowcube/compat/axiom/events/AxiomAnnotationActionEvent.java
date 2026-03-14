package net.hollowcube.compat.axiom.events;

import net.hollowcube.compat.axiom.data.annotations.actions.AnnotationAction;
import net.minestom.server.entity.Player;

import java.util.List;

/**
 * Called axiom performs an annotation action. ie. create, delete, move, clear, rotate.
 * <p>
 * When a module handles this event, it should set the {@link #handled} flag to true to prevent other modules from handling it.
 */
public final class AxiomAnnotationActionEvent implements AxiomEvent {

    private final Player player;

    private final List<AnnotationAction> actions;

    private boolean handled;

    public AxiomAnnotationActionEvent(Player player, List<AnnotationAction> actions) {
        this.player = player;
        this.actions = actions;
    }

    @Override
    public Player player() {
        return player;
    }

    public List<AnnotationAction> actions() {
        return actions;
    }

    public boolean isHandled() {
        return handled;
    }

    public void setHandled(boolean handled) {
        this.handled = handled;
    }

}
