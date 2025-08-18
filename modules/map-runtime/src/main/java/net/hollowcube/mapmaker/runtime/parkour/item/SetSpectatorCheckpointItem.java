package net.hollowcube.mapmaker.runtime.parkour.item;

import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.TempEffectApplicator;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.to_be_refactored.ActionBar;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.GenericTempActionBarProvider;
import net.kyori.adventure.key.Key;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.UUID;

public class SetSpectatorCheckpointItem extends ItemHandler {

    public static final Key ID = Key.key("mapmaker:set_spectator_checkpoint");
    public static final SetSpectatorCheckpointItem INSTANCE = new SetSpectatorCheckpointItem(ID);

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/set_checkpoint"));

    private SetSpectatorCheckpointItem(Key id) {
        super(id, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(Click click) {
        var player = click.player();
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;

        var added = switch (world.getPlayerState(player)) {
            case ParkourState.Testing(var saveState, var _) -> {
                TempEffectApplicator.applyCheckpoint(world, new ActionTriggerData(),
                        player, saveState, "temp-spec-" + UUID.randomUUID(), true);
                yield true;
            }
            case ParkourState.Spectating(var _, var gameState) -> {
                gameState.setPos(player.getPosition());
                yield true;
            }
            case null, default -> false;
        };
        if (added) {
            ActionBar.forPlayer(player).addProvider(new GenericTempActionBarProvider("Added temporary checkpoint!", 1000));
        }
    }

}
