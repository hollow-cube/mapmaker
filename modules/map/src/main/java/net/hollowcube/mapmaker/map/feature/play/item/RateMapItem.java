package net.hollowcube.mapmaker.map.feature.play.item;

import net.hollowcube.mapmaker.map.gui.RateMapView;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class RateMapItem extends ItemHandler {

    public static final String ID = "mapmaker:rate_map";
    public static final RateMapItem INSTANCE = new RateMapItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("play_maps/search/filter/best/icon"));

    private RateMapItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.DIAMOND;
    }

    @Override
    public int customModelData() {
        return SPRITE.cmd();
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = MapWorld.forPlayerOptional(player);
        if (world == null) return; // Sanity

        world.server().showView(player, c -> new RateMapView(c, world.map().id()));
    }

}
