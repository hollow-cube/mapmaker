package net.hollowcube.mapmaker.gui.map;

import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.api.maps.MapRating;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.panels.Button;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.panels.Text;
import net.hollowcube.mapmaker.player.PlayerData;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

import static net.hollowcube.mapmaker.gui.common.ExtraPanels.*;

public class RateMapView extends Panel {
    private final MapClient maps;
    private final MapData map;
    private final Consumer<MapRating.State> onChange;

    private final Button likeButton;
    private final Button dislikeButton;

    private MapRating.State ratingState;

    public RateMapView(
        @NotNull MapClient maps, @NotNull MapData map,
            @NotNull MapRating.State initialState, @NotNull Consumer<MapRating.State> onChange
    ) {
        super(9, 10);
        this.maps = maps;
        this.map = map;
        this.onChange = onChange;

        background("rate_map/container", -10, -31);
        add(0, 0, title("Rate Map"));

        add(0, 0, backOrClose());
        add(1, 0, info("map_rating"));
        var publishedId = MapData.formatPublishedId(map.publishedId());
        add(2, 0, new Text(null, 5, 1, publishedId)
                .align(Text.CENTER, Text.CENTER)
                .sprite("generic2/btn/default/5_1")
                .translationKey("gui.map_rating.map_id", publishedId));
        add(7, 0, new Button("gui.map_rating.report_map", 2, 1)
                .background("generic2/btn/default/2_1")
                .sprite("map_details/action/report", 15, 3)
            .onLeftClick(() -> host.pushView(new MapReportView(maps, map))));

        this.likeButton = add(1, 2, new Button(null, 3, 3)
                .onLeftClickAsync(() -> handleRatingStateChange(MapRating.State.LIKED)));
        this.dislikeButton = add(5, 2, new Button(null, 3, 3)
                .onLeftClickAsync(() -> handleRatingStateChange(MapRating.State.DISLIKED)));
        updateLocalRatingState(initialState);
    }

    private void handleRatingStateChange(@NotNull MapRating.State newState) {
        var resultState = this.ratingState == newState ? MapRating.State.UNRATED : newState;
        updateLocalRatingState(resultState);

        // Update the remote state async TODO: this should cancel prior request if there is already one out.
        var playerId = PlayerData.fromPlayer(host.player()).id();
        async(() -> {
            maps.setPlayerRating(this.map.id(), playerId, new MapRating(resultState, null));
            sync(() -> this.onChange.accept(newState));
        });
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
