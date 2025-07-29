package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.event.Map2Event;
import net.hollowcube.mapmaker.map.item.handler.ItemRegistry;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.thread.TickSchedulerThread;
import net.minestom.server.timer.Scheduler;
import org.jetbrains.annotations.*;

import java.util.Collection;

/// Any world running on mapmaker.
@NotNullByDefault
public sealed interface MapWorld2 permits AbstractMapWorld2 {

    /// Returns the _root_ map world for the given instance (if this instance is a map world).
    /// Note that in a case where there are sub-worlds, this will never return them (for example,
    /// the editing world will always be returned even if a test instance exists).
    static @Nullable MapWorld2 forInstance(Instance instance) {
        return instance.getTag(AbstractMapWorld2.ROOT_MAP_WORLD_TAG);
    }

    static @Nullable MapWorld2 forPlayer(Player player) {
        var instance = player.getInstance();
        if (instance == null) return null;

        // todo needs to get more complicated for sub-worlds
        return forInstance(instance);
    }

    MapServer server();

    MapData map();

    /// The instance this world exists in. Each world is always tied to a single instance,
    /// but multiple worlds can use the same instance.
    ///
    /// For example, a testing world for parkour maps are in the same instance as the editing world.
    Instance instance();

    /// An event node which is localized to only players actively in the world.
    ///
    /// This will trigger no matter the state of the player, you likely want more specific
    /// event nodes from the map implementation.
    EventNode<InstanceEvent> eventNode();

    default void callEvent(Map2Event event) {
        instance().eventNode().call(event);
    }

    default Scheduler scheduler() {
        return instance().scheduler();
    }

    /// An immutable view of the players in this world.
    ///
    /// This includes all players who are participating in any way, specifically:
    /// - Players testing an editing map will appear in the editing and testing worlds.
    /// - Players spectating a playing map will appear in the playing world.
    @UnmodifiableView Collection<Player> players();


    // REGION: Registries

    ItemRegistry itemRegistry();


    // REGION: Lifecycle

    @Blocking void configurePlayer(AsyncPlayerConfigurationEvent event);

    @NonBlocking void spawnPlayer(Player player);

    @NonBlocking void removePlayer(Player player);

    /// Called each tick on the tick scheduler thread. This means that it will always be
    /// after end of tick schedules on the instance, and before the next tick schedules.
    ///
    /// Generally should be used for transitioning players between states.
    default @NonBlocking void safePointTick() {
        if (!ServerFlag.INSIDE_TEST && !(Thread.currentThread() instanceof TickSchedulerThread)) // Sanity check
            throw new IllegalStateException("safePointTick called from non-scheduler thread!");
    }


    // TODO(new worlds): Re-add tag readable?

}
