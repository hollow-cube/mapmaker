package net.hollowcube.mapmaker.map;

import net.minestom.server.entity.Player;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNullByDefault;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collection;

/// Any world running on mapmaker.
@NotNullByDefault
public sealed interface MapWorld2 permits AbstractMapWorld2 {

    MapServer server();

    MapData map();

    /// The instance this world exists in. Each world is always tied to a single instance,
    /// but multiple worlds can use the same instance.
    ///
    /// For example, a testing world for parkour maps are in the same instance as the editing world.
    Instance instance();

    default Scheduler scheduler() {
        return instance().scheduler();
    }

    /// An immutable view of the players in this world.
    ///
    /// This includes all players who are participating in any way, specifically:
    /// - Players testing an editing map will appear in the editing and testing worlds.
    /// - Players spectating a playing map will appear in the playing world.
    @UnmodifiableView Collection<Player> players();

    @Blocking void configurePlayer(AsyncPlayerConfigurationEvent event);

    // TODO(new worlds): Re-add tag readable?

}
