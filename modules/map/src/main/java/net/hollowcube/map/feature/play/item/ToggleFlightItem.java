package net.hollowcube.map.feature.play.item;

import net.hollowcube.map.item.ItemHandler;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.minestom.server.item.Material;
import net.minestom.server.utils.NamespaceID;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class ToggleFlightItem extends ItemHandler {

    public static final String ID_ON = "mapmaker:toggle_flight_on";
    public static final String ID_OFF = "mapmaker:toggle_flight_off";
    public static final ToggleFlightItem INSTANCE_ON = new ToggleFlightItem(true);
    public static final ToggleFlightItem INSTANCE_OFF = new ToggleFlightItem(false);

    private static final BadSprite SPRITE_OFF = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/flight_off"));
    private static final BadSprite SPRITE_ON = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/flight_on"));

    private final boolean activeFlight;

    private ToggleFlightItem(boolean active) {
        super((active ? ID_ON : ID_OFF), RIGHT_CLICK_ANY);
        this.activeFlight = active;
    }

    @Override
    public @NotNull Material material() {
        return Material.DIAMOND;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        if (activeFlight) {
            player.setFlying(true);
            player.setAllowFlying(true);
            // Replace clicked item
            player.getInventory().setItemInMainHand(INSTANCE_OFF.buildItemStack(null));
        } else {
            player.setFlying(false);
            player.setAllowFlying(false);
            // Replace clicked item
            player.getInventory().setItemInMainHand(INSTANCE_ON.buildItemStack(null));
        }
    }

    @Override
    public int customModelData() {
        return activeFlight ? SPRITE_ON.cmd() : SPRITE_OFF.cmd();
    }
}
