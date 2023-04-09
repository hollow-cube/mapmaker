package net.hollowcube.map.world;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.feature.FeatureProvider;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.map.util.StringUtil;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.SaveState;
import net.hollowcube.world.BaseWorld;
import net.hollowcube.world.dimension.DimensionTypes;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventFilter;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.event.trait.PlayerEvent;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("UnstableApiUsage")
class EditingMapWorldNew implements InternalMapWorldNew {
    // If set, indicates that the player is an editor.
    private static final Tag<Boolean> TAG_EDITING = Tag.Boolean("editing").defaultValue(false);

    private final MapServer server;
    private final MapData map;
    private int flags = 0;

    private final BaseWorld baseWorld;
    private TestingMapWorldNew testWorld;

    private final List<FeatureProvider> enabledFeatures = new ArrayList<>();
    private final ItemRegistry itemRegistry;
    private final EventNode<InstanceEvent> eventNode = EventNode.event("world-local", EventFilter.INSTANCE, ev -> {
        if (ev instanceof PlayerEvent event) {
            return event.getPlayer().hasTag(TAG_EDITING);
        }
        return true;
    });

    public EditingMapWorldNew(@NotNull MapServer server, @NotNull MapData map) {
        this.server = server;
        this.map = map;
        this.flags |= FLAG_EDITING;

        var instance = new InstanceContainer(StringUtil.seededUUID(map.getId()), DimensionTypes.FULL_BRIGHT);
        this.baseWorld = new BaseWorld(server.worldManager(), map.getId(), instance);

        this.itemRegistry = new ItemRegistry();

        //todo how to move between editing and testing?
        /*
        Flow
        - removePlayer in editing world (still in instance)
        - acceptPlayer in testing world (still in instance)
        - simulate player spawn in instance event for testing world to handle spawn logic?
         */
    }

    @Override
    public @NotNull MapServer server() {
        return server;
    }

    @Override
    public @NotNull MapData map() {
        return map;
    }

    @Override
    public int flags() {
        return flags;
    }

    @Override
    public @NotNull ItemRegistry itemRegistry() {
        return itemRegistry;
    }

    @Override
    public void addScopedEventNode(@NotNull EventNode<InstanceEvent> eventNode) {

    }

    @Override
    public @NotNull ListenableFuture<Void> load() {
        return Futures.transformAsync(
                // Load the map data (eg blocks)
                JdkFutureAdapters.listenInPoolThread(baseWorld.loadWorld()),
                // Load the features. Notably after the map is loaded in case they depend on map data.
                unused -> {
                    var futures = new ArrayList<ListenableFuture<?>>();
                    for (var feature : server.features()) {
                        var future = feature.initMap(this);
                        if (future == null) continue;

                        futures.add(future);
                        enabledFeatures.add(feature);
                    }
                    return Futures.whenAllSucceed(futures).call(() -> null, Runnable::run);
                }
        );
    }

    @Override
    public @NotNull ListenableFuture<@NotNull SaveState> acceptPlayer(@NotNull Player player) {
        player.setTag(TAG_EDITING, true);
    }

    @Override
    public @NotNull ListenableFuture<Void> removePlayer(@NotNull Player player) {
        player.removeTag(TAG_EDITING);
    }

}
