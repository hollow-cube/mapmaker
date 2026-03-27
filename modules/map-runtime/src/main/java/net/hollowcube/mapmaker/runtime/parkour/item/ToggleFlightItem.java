package net.hollowcube.mapmaker.runtime.parkour.item;

import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.SpectateHelper;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ToggleFlightItem extends ItemHandler {
    public static final Key ID_ON = Key.key("mapmaker:toggle_flight_on");
    public static final Key ID_OFF = Key.key("mapmaker:toggle_flight_off");
    public static final ToggleFlightItem INSTANCE_ON = new ToggleFlightItem(true);
    public static final ToggleFlightItem INSTANCE_OFF = new ToggleFlightItem(false);

    private static final BadSprite SPRITE_OFF = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/flight_on"));
    private static final BadSprite SPRITE_ON = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/flight_off"));

    private final boolean activeFlight;

    private ToggleFlightItem(boolean active) {
        super((active ? ID_ON : ID_OFF), RIGHT_CLICK_ANY);
        this.activeFlight = active;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return activeFlight ? SPRITE_ON : SPRITE_OFF;
    }

    @Override
    protected void rightClicked(Click click) {
        var player = click.player();
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;

        SpectateHelper.toggleSpectatorFlight(world, player);
    }

}
