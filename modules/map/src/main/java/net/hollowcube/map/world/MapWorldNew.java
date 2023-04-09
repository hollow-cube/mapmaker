package net.hollowcube.map.world;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"PointlessBitwiseExpression"})

public interface MapWorldNew {

    int FLAG_EDITING = 1 << 0;
    int FLAG_PLAYING = 1 << 1;
    int FLAG_TESTING = 1 << 2;

    @NotNull MapServer server();
    @NotNull MapData map();
    int flags();

    @NotNull ItemRegistry itemRegistry();
    void addScopedEventNode(@NotNull EventNode<InstanceEvent> eventNode);

}
