package net.hollowcube.map.world;

import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NonBlocking;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface InternalMapWorld extends MapWorld {

    Tag<InternalMapWorld> SELF_TAG = Tag.Transient("mapworld");

    @NotNull Instance instance();

    @NotNull Point spawnPoint();

    /**
     * Loads the world. The world will not be marked active/ready for players until this future completes.
     */
    @Blocking
    void load();

    /**
     * Closes the world, including saving if relevant. When this method is called, the world is guaranteed not to have any new players added.
     * <p>
     * All players are guaranteed to be removed before calling this method.
     */
    @Blocking
    void close(boolean shutdown);

    @NonBlocking
    @Nullable MapWorld getMapForPlayer(@NotNull Player player);

    /**
     * Called as a player is entering the world, but before the player is added to the {@link net.minestom.server.instance.Instance}.
     * <p>
     *
     * @param player The player being added to the world
     */
    @Blocking
    void acceptPlayer(@NotNull Player player, boolean firstSpawn);

    /**
     * Called as a player is leaving the world, but before the player is removed from the {@link net.minestom.server.instance.Instance}.
     * <p>
     * Note that the player may be removed from the instance before the returned future completes.
     *
     * @param player The player being removed from the world
     */
    @Blocking
    void removePlayer(@NotNull Player player);

}
