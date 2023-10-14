package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class MapDetailsItem extends ItemHandler {

    public static final String ID = "mapmaker:map_details";
    public static final MapDetailsItem INSTANCE = new MapDetailsItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/map_details"));

    private MapDetailsItem() {
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
        player.sendMessage("TODO: show map details GUI");
    }

}
