package net.hollowcube.mapmaker.runtime.parkour.item;

import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.SpectateHelper;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ToggleGameplayItem extends ItemHandler {
    public static final Key ID_ON = Key.key("mapmaker:toggle_gameplay_on");
    public static final Key ID_OFF = Key.key("mapmaker:toggle_gameplay_off");
    public static final ToggleGameplayItem INSTANCE_ON = new ToggleGameplayItem(true);
    public static final ToggleGameplayItem INSTANCE_OFF = new ToggleGameplayItem(false);

    private static final BadSprite SPRITE_OFF = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/gameplay_off"));
    private static final BadSprite SPRITE_ON = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/gameplay_on"));

    private final boolean enableGameplay;

    private ToggleGameplayItem(boolean active) {
        super(active ? ID_ON : ID_OFF, RIGHT_CLICK_ANY);
        this.enableGameplay = active;
    }

    @Override
    public @Nullable BadSprite sprite() {
        return enableGameplay ? SPRITE_ON : SPRITE_OFF;
    }

    @Override
    protected void rightClicked(Click click) {
        var player = click.player();
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;

        if (player.isSneaking()) {
            SpectateHelper.toggleSpectatorFlight(world, player);
            return;
        }

        SpectateHelper.changeGameplaySettingsState(world, player, enableGameplay ? TriState.TRUE : TriState.FALSE);
        click.update(enableGameplay
                ? ToggleGameplayItem.INSTANCE_OFF.getItemStack()
                : ToggleGameplayItem.INSTANCE_ON.getItemStack());
    }
}
