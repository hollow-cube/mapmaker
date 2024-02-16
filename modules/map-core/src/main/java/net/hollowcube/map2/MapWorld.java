package net.hollowcube.map2;

import net.hollowcube.map2.biome.BiomeContainer;
import net.hollowcube.map2.item.handler.ItemRegistry;
import net.hollowcube.mapmaker.map.MapData;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventNode;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.trait.InstanceEvent;
import net.minestom.server.instance.Instance;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Objects;

public sealed interface MapWorld permits AbstractMapWorld {

    @NonBlocking
    static @NotNull MapWorld forPlayer(@NotNull Player player) {
        return Objects.requireNonNull(forPlayerOptional(player));
    }

    @NonBlocking
    static @Nullable MapWorld forPlayerOptional(@NotNull Player player) {
        if (player.getInstance() == null) return null;
        var world = unsafeFromInstance(player.getInstance());
        if (world instanceof AbstractMapWorld w1)
            return w1.getMapForPlayer(player);
        return null;
    }

    @NonBlocking
    static @Nullable MapWorld unsafeFromInstance(@Nullable Instance instance) {
        if (instance == null) return null;
        return instance.getTag(AbstractMapWorld.SELF_TAG);
    }

    @NotNull MapServer server();
    @NotNull MapData map();

    @NotNull Instance instance();
    default @NotNull Pos spawnPoint(@NotNull Player player) {
        return map().settings().getSpawnPoint();
    }

    @NotNull ItemRegistry itemRegistry();
    @NotNull BiomeContainer biomes();
    // AnimationManager, etc.

    @NotNull Collection<Player> players();
    @NotNull Collection<Player> spectators();

    @Blocking
    void configurePlayer(@NotNull AsyncPlayerConfigurationEvent event);
    @Blocking
    void addPlayer(@NotNull Player player);
    @Blocking
    void addSpectator(@NotNull Player player);
    @Blocking
    void removePlayer(@NotNull Player player);

    default boolean isPlaying(@NotNull Player player) {
        return players().contains(player);
    }
    default boolean isSpectating(@NotNull Player player) {
        return spectators().contains(player);
    }
    default boolean canEdit(@NotNull Player player) {
        return false; // Worlds are read-only by default
    }

    /**
     * Gets the {@link EventNode} for this world. This node only triggers player events for players
     * who are actively playing, NOT spectators. If listening for spectators is required, an event
     * should be registered on the instance event node directly.
     *
     * @return An event node for the active players in the world.
     */
    @NotNull EventNode<InstanceEvent> eventNode();
    default void callEvent(@NotNull InstanceEvent event) {
        eventNode().call(event);
    }

}
