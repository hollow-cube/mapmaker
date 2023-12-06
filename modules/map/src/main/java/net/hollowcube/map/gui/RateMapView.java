package net.hollowcube.map.gui;

import net.hollowcube.canvas.Label;
import net.hollowcube.canvas.View;
import net.hollowcube.canvas.annotation.ContextObject;
import net.hollowcube.canvas.annotation.OutletGroup;
import net.hollowcube.canvas.internal.Context;
import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.map.feature.play.MapRatingFeatureProvider;
import net.hollowcube.mapmaker.map.MapPlayerData;
import net.hollowcube.mapmaker.map.MapService;
import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.entity.Player;
import org.jetbrains.annotations.Blocking;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class RateMapView extends View {

    private @ContextObject MapService mapService;
    private @ContextObject Player player;

    private @OutletGroup("rating_\\d") Label[] ratingButtons;

    private final String mapId;

    public RateMapView(@NotNull Context context, @NotNull String mapId) {
        super(context);
        this.mapId = mapId;

        // If the player has an existing rating, load it.
        async(() -> {
            var lastRating = FutureUtil.getUnchecked(player.getTag(MapRatingFeatureProvider.LAST_RATING_TAG));
            if (lastRating == null || lastRating == -1) return;

//            ratingButtons[lastRating - 1];
            System.out.println("rating button " + lastRating + " should be selected");
        });

        // Add handlers dynamically for each button
        for (int i = 0; i < ratingButtons.length; i++) {
            int rating = i + 1;
            addAsyncActionHandler(
                    String.format("rating_%d", rating),
                    Label.ActionHandler.lmb(player -> handleSelectRating(player, rating))
            );
        }
    }

    @Blocking
    private void handleSelectRating(@NotNull Player player, int rating) {
        player.closeInventory();

        try {
            var playerData = MapPlayerData.fromPlayer(player);
            mapService.setMapRating(mapId, playerData.id(), rating);

            // Update the local rating
            player.setTag(MapRatingFeatureProvider.LAST_RATING_TAG, CompletableFuture.completedFuture(rating));

            player.sendMessage(Component.translatable("gui.rate_map.rating_set", Component.text(rating))); //todo rating needs to be converted to a name
        } catch (Exception e) {
            MinecraftServer.getExceptionManager().handleException(e);
            player.sendMessage(Component.translatable("command.generic.unknown_error"));
        }
    }

}
