package net.hollowcube.mapmaker.map.feature.play.item;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.mapmaker.map.feature.play.handlers.SpectateHandler;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ReturnToCheckpointItem extends ItemHandler {
    private static final TagCooldown CONFIRM_COOLDOWN = new TagCooldown("mapmaker:play/checkpoint_item_confirm", 2000);
    private static final int MIN_RESET_TIME = 1 * 60 * 1000; // 1 minute

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/return_to_checkpoint"));
    public static final String ID = "mapmaker:return_to_checkpoint";
    public static final ReturnToCheckpointItem INSTANCE = new ReturnToCheckpointItem();

    private ReturnToCheckpointItem() {
        super(ID, RIGHT_CLICK_ANY);
    }

    @Override
    public @Nullable BadSprite sprite() {
        return SPRITE;
    }

    @Override
    protected void rightClicked(@NotNull Click click) {
        var player = click.player();
        var world = MapWorld.forPlayerOptional(player);

        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState != null && requireConfirmation(player, saveState) && CONFIRM_COOLDOWN.test(player)) {
            // This only happens without a checkpoint, so it acts as a full reset anyway.
            player.sendMessage(Component.translatable("item.mapmaker.reset_savestate.confirm"));
            return;
        }

        if (world instanceof PlayingMapWorld || world instanceof TestingMapWorld) {
            EventDispatcher.call(new MapPlayerResetEvent(player, world, true));
        }
    }

    private boolean requireConfirmation(@NotNull Player player, @NotNull SaveState saveState) {
        // If you have a spectator checkpoint, never require confirmation
        if (SpectateHandler.getCheckpoint(player) != null) return false;
        // The reset item requires confirm click if the playtime is > MIN_RESET_TIME and the player does _not_ have a checkpoint
        if (saveState.getRealPlaytime() <= MIN_RESET_TIME) return false;
        var playState = saveState.state(PlayState.class);
        // If you have a last state you have a checkpoint.
        return playState.lastState() == null;
    }

}
