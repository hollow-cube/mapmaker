package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ToggleFlightItem extends ItemHandler {

    public static final String ID = "mapmaker:toggle_flight";
    public static final ToggleFlightItem INSTANCE = new ToggleFlightItem();

    private static final BadSprite SPRITE_OFF = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/flight_off"));
    private static final BadSprite SPRITE_ON = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/flight_on"));

    private ToggleFlightItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @NotNull Material material() {
        return Material.DIAMOND;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        if (player.isAllowFlying()) {
            player.setFlying(false);
            player.setAllowFlying(false);
        } else {
            player.setFlying(true);
            player.setAllowFlying(true);
        }
    }

    @Override
    public int customModelData() {
        return SPRITE_ON.cmd(); //todo how to switch
    }
    // TODO also needs to switch translation keys so it says something in the item name like Flight: ON or Flight: OFF
}
