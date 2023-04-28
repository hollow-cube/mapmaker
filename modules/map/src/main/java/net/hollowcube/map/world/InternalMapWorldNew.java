package net.hollowcube.map.world;

import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.mapmaker.model.SaveState;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

interface InternalMapWorldNew extends MapWorldNew {

    Tag<InternalMapWorldNew> SELF_TAG = Tag.Transient("mapworld");

    @NotNull Instance instance();
    @NotNull Point spawnPoint();

    /**
     * Loads the world. The world will not be marked active/ready for players until this future completes.
     *
     * @return A future which completes when the world is ready for players.
     */
    @Blocking void load();

    /**
     * Closes thr world, including saving if relevant. When this method is called, the world is guaranteed not to have any new players added.
     * <p>
     * All players are guaranteed to be removed before calling this method.
     *
     * @return A future which completes when the world is closed.
     */
    @Blocking void close();

    /**
     * Called as a player is entering the world, but before the player is added to the {@link net.minestom.server.instance.Instance}.
     * <p>
     * Note that the player may be added to the instance before the returned future completes.
     *
     * @param player The player being added to the world
     * @return A future that completes when the player may begin interacting with the world.
     */
    @Blocking void acceptPlayer(@NotNull Player player);

    /**
     * Called as a player is leaving the world, but before the player is removed from the {@link net.minestom.server.instance.Instance}.
     * <p>
     * Note that the player may be removed from the instance before the returned future completes.
     *
     * @param player The player being removed from the world
     * @return A future that completes when the player has been completely removed from the world.
     */
    @Blocking void removePlayer(@NotNull Player player);

}
