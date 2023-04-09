package net.hollowcube.map.world;

import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.map.MapServer;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.mapmaker.model.MapData;
import net.hollowcube.mapmaker.model.SaveState;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;

class TestingMapWorldNew implements InternalMapWorldNew {

    private final EditingMapWorldNew parent;

    private final ItemRegistry itemRegistry;

    public TestingMapWorldNew(@NotNull EditingMapWorldNew parent) {
        this.parent = parent;

        this.itemRegistry = new ItemRegistry();
    }

    @Override
    public @NotNull MapServer server() {
        return parent.server();
    }

    @Override
    public @NotNull MapData map() {
        return parent.map();
    }

    @Override
    public int flags() {
        return parent.flags() | MapWorldNew.FLAG_TESTING;
    }

    @Override
    public @NotNull ItemRegistry itemRegistry() {
        return itemRegistry;
    }

    @Override
    public void addScopedEventNode(@NotNull EventNode<InstanceEvent> eventNode) {

    }

    @Override
    public @NotNull Instance instance() {
        return null;
    }

    @Override
    public @NotNull Point spawnPoint() {
        return null;
    }

    @Override
    public @NotNull ListenableFuture<Void> load() {
        return null;
    }

    @Override
    public @NotNull ListenableFuture<Void> close() {
        return null;
    }

    @Override
    public @NotNull ListenableFuture<@NotNull SaveState> acceptPlayer(@NotNull Player player) {
        return null;
    }

    @Override
    public @NotNull ListenableFuture<Void> removePlayer(@NotNull Player player) {
        return null;
    }
}
