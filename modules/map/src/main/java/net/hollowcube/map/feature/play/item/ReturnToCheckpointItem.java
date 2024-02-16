package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.map.world.PlayingMapWorld;
import net.hollowcube.map.world.TestingMapWorld;
import net.hollowcube.map2.MapWorld;
import net.hollowcube.map2.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ReturnToCheckpointItem extends ItemHandler {

    public static final String ID = "mapmaker:return_to_checkpoint";
    public static final ReturnToCheckpointItem INSTANCE = new ReturnToCheckpointItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/return_to_checkpoint"));

    private ReturnToCheckpointItem() {
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
        if (world instanceof PlayingMapWorld || world instanceof TestingMapWorld) {
            EventDispatcher.call(new MapPlayerResetEvent(player, world, true));
        }
    }

}
