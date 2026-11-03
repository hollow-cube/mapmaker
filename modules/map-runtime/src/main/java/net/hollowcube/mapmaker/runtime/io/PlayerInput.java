package net.hollowcube.mapmaker.runtime.io;

import net.hollowcube.mapmaker.runtime.PlayState;
import net.minestom.server.entity.Player;

/// An action, for now
public interface PlayerInput {

    default void applyTo(Player player, PlayState state) {}
}
