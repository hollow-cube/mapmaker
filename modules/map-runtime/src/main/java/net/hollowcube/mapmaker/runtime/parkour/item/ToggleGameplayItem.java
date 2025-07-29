package net.hollowcube.mapmaker.runtime.parkour.item;

import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.SaveStateType;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld2;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class ToggleGameplayItem extends ItemHandler {
    public static final Key ID_ON = Key.key("mapmaker:toggle_gameplay_on");
    public static final Key ID_OFF = Key.key("mapmaker:toggle_gameplay_off");
    public static final ToggleGameplayItem INSTANCE_ON = new ToggleGameplayItem(true);
    public static final ToggleGameplayItem INSTANCE_OFF = new ToggleGameplayItem(false);

    private static final BadSprite SPRITE_OFF = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/exit_spectator"));
    private static final BadSprite SPRITE_ON = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/enter_spectator"));

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
        var world = ParkourMapWorld2.forPlayer(player);
        if (world == null) return;

        var nextState = switch (world.getPlayerState(player)) {
            case ParkourState.Spectating(var playState, var _) when enableGameplay -> {
                var fakePlayState = playState.copy();
                fakePlayState.setPos(player.getPosition());
                // Set this state to last state to create a checkpoint
                fakePlayState.setLastState(fakePlayState.copy());
                var saveState = new SaveState(UUID.randomUUID().toString(), player.getUuid().toString(), world.map().id(),
                        SaveStateType.PLAYING, PlayState.SERIALIZER, fakePlayState);
                yield new ParkourState.Playing(saveState, false);
            }
            case ParkourState.Playing(var saveState, var _) when !enableGameplay ->
                    new ParkourState.Spectating(saveState.state(PlayState.class), false);
            case null, default -> null;
        };
        if (nextState != null) world.changePlayerState(player, nextState);
    }

}
