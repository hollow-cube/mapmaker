package net.hollowcube.mapmaker.runtime.parkour.item;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.api.maps.MapClient;
import net.hollowcube.mapmaker.api.maps.MapRating;
import net.hollowcube.mapmaker.gui.map.RateMapView;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.panels.Panel;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import net.minestom.server.entity.Player;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

public class RateMapItem extends ItemHandler {
    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/rate_map"));
    public static final Key ID = Key.key("mapmaker:rate_map");
    public static final RateMapItem INSTANCE = new RateMapItem();

    public static final Tag<Future<@Nullable MapRating>> LAST_RATING_TAG = Tag.Transient("map:last_rating");

    public static boolean isMapRatable(MapWorld world) {
        return world.map().isPublished() && world instanceof ParkourMapWorld;
    }

    public static void initLastRating(MapClient maps, Player player, MapData map) {
        player.setTag(LAST_RATING_TAG, FutureUtil.fork(() -> {
            try {
                return maps.getPlayerRating(map.id(), player.getUuid().toString());
            } catch (Exception e) {
                ExceptionReporter.reportException(e, player);
                // It's fine to default to a new rating since its valid to overwrite a rating anyway.
                return new MapRating();
            }
        }));
    }

    private RateMapItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(Click click) {
        var player = click.player();
        var world = MapWorld.forPlayer(player);
        if (world == null) return;

        var initialState = MapRating.State.UNRATED;
        var lastRatingFuture = player.getTag(LAST_RATING_TAG);
        if (lastRatingFuture != null)
            initialState = Objects.requireNonNullElse(lastRatingFuture.resultNow(), new MapRating()).state();
        Panel.open(player, new RateMapView(world.server().api().maps, world.map(), initialState, newState ->
                player.setTag(LAST_RATING_TAG, CompletableFuture.completedFuture(new MapRating(newState, null)))));
    }

}
