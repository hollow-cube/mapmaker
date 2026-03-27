package net.hollowcube.mapmaker.editor;

import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;

/// Exposes a direct join method for "sub" worlds, aka a test parkour world.
public interface SubWorld {

    /// Directly adds the player to the map with the given state. The player
    /// must already be in the instance and not in the map.
    ///
    /// Note that this should be used very much with caution. Without entering
    /// the configuration state we cannot change things like registries.
    ///
    /// Currently, this is in use for entering testing mode.
    @Blocking
    void addPlayerDirect(Player player, Runnable callback);

}
