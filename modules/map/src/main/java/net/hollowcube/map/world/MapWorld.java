package net.hollowcube.map.world;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.feature.mapsize.MapSizeData;
import net.hollowcube.map.item.ItemRegistry;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.Set;

import static net.hollowcube.map.world.InternalMapWorld.SELF_TAG;

@SuppressWarnings({"PointlessBitwiseExpression"})
public interface MapWorld {

    static @NonBlocking
    @NotNull MapWorld forPlayer(@NotNull Player player) {
        return Objects.requireNonNull(forPlayerOptional(player));
    }

    static @NonBlocking
    @Nullable MapWorld forPlayerOptional(@NotNull Player player) {
        if (player.getInstance() == null) return null;
        var world = unsafeFromInstance(player.getInstance());
        if (world instanceof InternalMapWorld internalWorld) {
            return internalWorld.getMapForPlayer(player);
        }
        return null;
    }

    static @NonBlocking
    @Nullable MapWorld unsafeFromInstance(@Nullable Instance instance) {
        if (instance == null) return null;
        return instance.getTag(SELF_TAG);
    }

    int FLAG_EDITING = 1 << 0;
    int FLAG_PLAYING = 1 << 1;
    int FLAG_TESTING = 1 << 2;

    @NotNull MapServer server();

    @NotNull Instance instance();

    @NotNull MapData map();

    int flags();

    @NotNull ItemRegistry itemRegistry();

    void addScopedEventNode(@NotNull EventNode<InstanceEvent> eventNode);

    void removeScopedEventNode(@NotNull EventNode<InstanceEvent> eventNode);

    @NotNull Set<Player> players();

    @NotNull MapSizeData mapSizeData();
    @Blocking void setMapSizeData(@NotNull MapSizeData mapSizeData);
}
