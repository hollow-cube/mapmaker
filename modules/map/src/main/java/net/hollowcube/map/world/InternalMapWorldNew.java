package net.hollowcube.map.world;

import com.google.common.util.concurrent.ListenableFuture;
import net.hollowcube.mapmaker.model.SaveState;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

interface InternalMapWorldNew extends MapWorldNew {

    /**
     * Loads the world. The world will not be marked active/ready for players until this future completes.
     *
     * @return A future which completes when the world is ready for players.
     */
    @NotNull ListenableFuture<Void> load();

    /**
     * Called as a player is entering the world, but before the player is added to the {@link net.minestom.server.instance.Instance}.
     * <p>
     * Note that the player may be added to the instance before the returned future completes.
     *
     * @param player The player being added to the world
     * @return A future that completes when the player may begin interacting with the world.
     */
    @NotNull ListenableFuture<@NotNull SaveState> acceptPlayer(@NotNull Player player);

    /**
     * Called as a player is leaving the world, but before the player is removed from the {@link net.minestom.server.instance.Instance}.
     * <p>
     * Note that the player may be removed from the instance before the returned future completes.
     *
     * @param player The player being removed from the world
     * @return A future that completes when the player has been completely removed from the world.
     */
    @NotNull ListenableFuture<Void> removePlayer(@NotNull Player player);

}
