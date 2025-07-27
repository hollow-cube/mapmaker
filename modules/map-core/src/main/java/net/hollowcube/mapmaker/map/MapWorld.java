package net.hollowcube.mapmaker.map;

import net.hollowcube.mapmaker.map.biome.BiomeContainer;
import net.hollowcube.mapmaker.map.entity.object.ObjectEntityHandlerRegistry;
import net.hollowcube.mapmaker.map.event.trait.Map2Event;
import net.hollowcube.mapmaker.map.item.handler.ItemRegistry;
import net.hollowcube.mapmaker.map.util.spatial.Octree;
import net.minestom.server.ServerFlag;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.tag.TagReadable;
import net.minestom.server.thread.TickSchedulerThread;
import net.minestom.server.timer.Scheduler;
import net.minestom.server.timer.TaskSchedule;
import org.jetbrains.annotations.*;

import java.util.Collection;
import java.util.concurrent.CompletableFuture;

/// Any world running on mapmaker.
@NotNullByDefault
public sealed interface MapWorld extends TagReadable permits AbstractMapWorld {

    /// Returns the _root_ map world for the given instance (if this instance is a map world).
    /// Note that in a case where there are sub-worlds, this will never return them (for example,
    /// the editing world will always be returned even if a test instance exists).
    static @Nullable MapWorld forInstance(@Nullable Instance instance) {
        if (instance == null) return null;
        return instance.getTag(AbstractMapWorld.ROOT_MAP_WORLD_TAG);
    }

    static @Nullable MapWorld forPlayer(Player player) {
        return forPlayer(MapWorld.class, player);
    }

    static <W extends MapWorld> @Nullable W forPlayer(Class<W> type, Player player) {
        var instance = player.getInstance();
        if (instance == null) return null;

        var root = forInstance(instance);
        if (root == null) return null;

        var canonical = root.canonicalWorld(player, type);
        return type.isInstance(canonical) ? type.cast(canonical) : null;
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

    Octree collisionTree();

    /// Queues a rebuild of the collision tree, will be processed sometime in the future up to the implementation.
    void queueCollisionTreeRebuild();

    //region Registries

    BiomeContainer biomes();

    ItemRegistry itemRegistry();

    ObjectEntityHandlerRegistry objectEntityHandlers();

    //endregion

    //region Player Lifecycle

    /// An immutable view of the players in this world.
    ///
    /// This includes all players who are participating in any way, specifically:
    /// - Players testing an editing map will appear in the editing and testing worlds.
    /// - Players spectating a playing map will appear in the playing world.
    @UnmodifiableView Collection<Player> players();

    @Blocking void configurePlayer(AsyncPlayerConfigurationEvent event);

    @NonBlocking void spawnPlayer(Player player);

    /// @return a future which completes _during a safe point tick_.
    CompletableFuture<Void> scheduleRemovePlayer(Player player);

    @NonBlocking void removePlayer(Player player);

    @NonBlocking
    default @Nullable MapWorld canonicalWorld(Player player, Class<? extends MapWorld> type) {
        return type.isAssignableFrom(getClass()) ? this : null;
    }

    /// Recognizing that this is a terrible function that obviously should not exist,
    /// it is being added in the phase of the map rework branch where it really just needs
    /// to be done and we can come back to figure out a better mechanism here later.
    @Deprecated
    default boolean shouldTriggerDripleaf(Player player) {
        return false;
    }

    /// Called each tick on the tick scheduler thread. This means that it will always be
    /// after end of tick schedules on the instance, and before the next tick schedules.
    ///
    /// Generally should be used for transitioning players between states.
    default @NonBlocking TaskSchedule safePointTick() {
        if (!ServerFlag.INSIDE_TEST && !(Thread.currentThread() instanceof TickSchedulerThread)) // Sanity check
            throw new IllegalStateException("safePointTick called from non-scheduler thread! " + Thread.currentThread());
        return TaskSchedule.nextTick();
    }

    //endregion

    //region Tag implementation

    @Override
    default <T> @UnknownNullability T getTag(Tag<T> tag) {
        return instance().getTag(tag);
    }

    @Override
    default boolean hasTag(Tag<?> tag) {
        return instance().hasTag(tag);
    }

    // TODO: not a fan, would like to move marker handler editing behavior out of ObjectEntity. but currently its there.
    @Deprecated
    default boolean canEdit(@Nullable Player player) {
        return false; // default read only
    }

    //endregion

}
