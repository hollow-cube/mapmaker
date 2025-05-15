package net.hollowcube.mapmaker.map.feature.play.item;

import net.hollowcube.mapmaker.gui.map.RateMapView;
import net.hollowcube.mapmaker.map.MapRating;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.feature.play.MapRatingFeatureProvider;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class RateMapItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/rate_map"));
    public static final String ID = "mapmaker:rate_map";
    public static final RateMapItem INSTANCE = new RateMapItem();

    private RateMapItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return; // Sanity

        var initialState = MapRating.State.UNRATED;
        var lastRatingFuture = player.getTag(MapRatingFeatureProvider.LAST_RATING_TAG);
        if (lastRatingFuture != null)
            initialState = Objects.requireNonNullElse(lastRatingFuture.resultNow(), new MapRating()).state();
        Panel.open(player, new RateMapView(world.server().mapService(), world.map(), initialState, newState ->
                player.setTag(MapRatingFeatureProvider.LAST_RATING_TAG, CompletableFuture.completedFuture(new MapRating(newState, null)))));
    }

}
