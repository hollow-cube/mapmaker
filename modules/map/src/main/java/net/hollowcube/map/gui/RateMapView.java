package net.hollowcube.map.gui;

import net.hollowcube.canvas.Switch;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.Action;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.Outlet;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.feature.play.MapRatingFeatureProvider;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapRating;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class RateMapView extends View {
    private static final int OFF = 0;
    private static final int ON = 1;

    private @ContextObject MapService mapService;
    private @ContextObject Player player;

    private @Outlet("like_switch") Switch likeSwitch;
    private @Outlet("dislike_switch") Switch dislikeSwitch;

    private final String mapId;
    private MapRating rating = new MapRating();

    public RateMapView(@NotNull Context context, @NotNull String mapId) {
        super(context);
        this.mapId = mapId;

        // If the player has an existing rating, load it.
        async(() -> {
            var lastRating = FutureUtil.getUnchecked(player.getTag(MapRatingFeatureProvider.LAST_RATING_TAG));
            if (lastRating == null) return;
            updateLocalRating(lastRating);
        });
    }

    @Action("like_off")
    public void handleLike(@NotNull Player player) {
        player.closeInventory();

        // No need to update local because the GUI is closed
        async(() -> updateRemoteRating(new MapRating(MapRating.State.LIKED, rating.comment())));
    }

    @Action("dislike_off")
    public void handleDislike(@NotNull Player player) {
        player.closeInventory();

        // No need to update local because the GUI is closed
        async(() -> updateRemoteRating(new MapRating(MapRating.State.DISLIKED, rating.comment())));
    }

    /**
     * Updates the GUI to reflect the new rating and keeps it in local state but does NOT submit it remotely to be updated.
     * <p>
     * TODO: probably will use this when updating your comment.
     */
    private void updateLocalRating(@NotNull MapRating rating) {
        this.rating = rating;
        likeSwitch.setOption(rating.state() == MapRating.State.LIKED ? ON : OFF);
        dislikeSwitch.setOption(rating.state() == MapRating.State.DISLIKED ? ON : OFF);
    }

    private void updateRemoteRating(@NotNull MapRating rating) {
        try {
            var playerData = MapPlayerData.fromPlayer(player);
            mapService.setMapRating(mapId, playerData.id(), rating);

            // Update the cached rating on the player.
            player.setTag(MapRatingFeatureProvider.LAST_RATING_TAG, CompletableFuture.completedFuture(rating));
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
            player.sendMessage(Component.translatable("command.generic.unknown_error"));
        }
    }

}
