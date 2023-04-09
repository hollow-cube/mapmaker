package net.hollowcube.map.world;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
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
}
