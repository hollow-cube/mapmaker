package net.hollowcube.mapmaker.gui.map.details;

import net.hollowcube.mapmaker.map.MapRating;
import net.hollowcube.mapmaker.map.MapService;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.InventoryHost;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.player.PlayerData;
import org.jetbrains.annotations.NotNull;

class MapDetailsRatePanel extends Panel {
    private final MapService mapService;
    private final String mapId;

    private final Button likeButton;
    private final Button dislikeButton;

    private MapRating.State ratingState;

    public MapDetailsRatePanel(@NotNull MapService mapService, @NotNull String mapId) {
        super(9, 4);
        this.mapService = mapService;
        this.mapId = mapId;

        background("map_details/rate/container");

        this.likeButton = add(1, 1, new Button(null, 3, 3)
                .onLeftClickAsync(() -> handleRatingStateChange(MapRating.State.LIKED)));
        this.dislikeButton = add(5, 1, new Button(null, 3, 3)
                .onLeftClickAsync(() -> handleRatingStateChange(MapRating.State.DISLIKED)));
        updateLocalRatingState(MapRating.State.UNRATED);
    }

    @Override
    protected void mount(@NotNull InventoryHost host, boolean isInitial) {
        super.mount(host, isInitial);
        if (!isInitial) return;

        var playerId = PlayerData.fromPlayer(host.player()).id();
        async(() -> {
            var rating = mapService.getMapRating(this.mapId, playerId);
            sync(() -> updateLocalRatingState(rating.state()));
        });
    }

    private void handleRatingStateChange(@NotNull MapRating.State newState) {
        var resultState = this.ratingState == newState ? MapRating.State.UNRATED : newState;
        updateLocalRatingState(resultState);

        // Update the remote state async TODO: this should cancel prior request if there is already one out.
        var playerId = PlayerData.fromPlayer(host.player()).id();
        async(() -> mapService.setMapRating(this.mapId, playerId, new MapRating(resultState, null)));
    }

    private void updateLocalRatingState(@NotNull MapRating.State newState) {
        if (this.ratingState == newState) return;
        this.ratingState = newState;

        this.likeButton.translationKey("gui.map_rating.like_" + (newState == MapRating.State.LIKED ? "on" : "off"));
        this.likeButton.sprite("rate_map/like_" + (newState == MapRating.State.LIKED ? "on" : "off"));

        this.dislikeButton.translationKey("gui.map_rating.dislike_" + (newState == MapRating.State.DISLIKED ? "on" : "off"));
        this.dislikeButton.sprite("rate_map/dislike_" + (newState == MapRating.State.DISLIKED ? "on" : "off"));
    }
}
