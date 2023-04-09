package net.hollowcube.map.world;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.mapmaker.model.MapData;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

import static net.hollowcube.map.world.InternalMapWorldNew.SELF_TAG;

@SuppressWarnings({"PointlessBitwiseExpression"})
public interface MapWorldNew {

    static @NotNull MapWorldNew fromInstance(@NotNull Instance instance) {
        return Objects.requireNonNull(optionalFromInstance(instance), "instance is not a map world");
    }

    static @Nullable MapWorldNew optionalFromInstance(@Nullable Instance instance) {
        if (instance == null) return null;
        return instance.getTag(SELF_TAG);
    }

    int FLAG_EDITING = 1 << 0;
    int FLAG_PLAYING = 1 << 1;
    int FLAG_TESTING = 1 << 2;

    @NotNull MapServer server();
    @NotNull MapData map();
    int flags();

    @NotNull ItemRegistry itemRegistry();
    void addScopedEventNode(@NotNull EventNode<InstanceEvent> eventNode);

}
