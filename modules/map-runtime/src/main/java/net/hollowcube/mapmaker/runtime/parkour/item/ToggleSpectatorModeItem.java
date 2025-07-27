package net.hollowcube.mapmaker.runtime.parkour.item;

import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.runtime.ParkourMapWorld2;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ToggleSpectatorModeItem extends ItemHandler {
    public static final Key ID_ON = Key.key("mapmaker:toggle_spectator_on");
    public static final Key ID_OFF = Key.key("mapmaker:toggle_spectator_off");
    public static final ToggleSpectatorModeItem INSTANCE_ON = new ToggleSpectatorModeItem(true);
    public static final ToggleSpectatorModeItem INSTANCE_OFF = new ToggleSpectatorModeItem(false);

    private static final BadSprite SPRITE_OFF = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/exit_spectator"));
    private static final BadSprite SPRITE_ON = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/enter_spectator"));

    private final boolean activeSpectator;

    private ToggleSpectatorModeItem(boolean active) {
        super(active ? ID_ON : ID_OFF, RIGHT_CLICK_ANY);
        this.activeSpectator = active;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return activeSpectator ? SPRITE_ON : SPRITE_OFF;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = ParkourMapWorld2.forPlayer(player);
        if (world == null) return;

        world.changePlayerState(player, activeSpectator ? new ParkourState.Spectating(false)
                : new ParkourState.Playing(Objects.requireNonNull(world.getSaveState(player))));
    }

}
