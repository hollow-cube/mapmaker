package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.MapServer;
import net.hollowcube.map.gui.RateMapView;
import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.map.world.MapWorld;
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
        var server = MapServer.StaticAbuse.instance;

        var map = MapWorld.forPlayer(player).map();
        server.newOpenGUI(player, c -> new RateMapView(c, map.id()));
    }

}
