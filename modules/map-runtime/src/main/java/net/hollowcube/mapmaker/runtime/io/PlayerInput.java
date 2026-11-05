package net.hollowcube.mapmaker.runtime.io;

import net.hollowcube.mapmaker.runtime.PlayState;
import net.minestom.server.entity.Player;

/// An action, for now
public interface PlayerInput {

    default void applyTo(Player player, PlayState state) {}

    default void deliver(Dispatch.Delivery delivery) {
        applyTo(delivery.player(), delivery.state());
    }

    /// True for inputs that queue a change to the player's state (finishing, resetting), after which re-applying the
    /// old state to the player would be wrong.
    default boolean replacesState() {
        return false;
    }
}
