package net.hollowcube.mapmaker.map.feature.play.item;

import net.hollowcube.common.util.FutureUtil;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.map.event.vnext.MapPlayerResetEvent;
import net.hollowcube.mapmaker.map.item.handler.ItemHandler;
import net.hollowcube.mapmaker.map.world.PlayingMapWorld;
import net.hollowcube.mapmaker.map.world.TestingMapWorld;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.to_be_refactored.BadSprite;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;
import net.minestom.server.event.EventDispatcher;
import net.minestom.server.item.Material;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

import static net.hollowcube.mapmaker.map.feature.play.item.SetSpectatorCheckpointItem.SPECTATOR_CHECKPOINT;

public class ResetSaveStateItem extends ItemHandler {
    private static final TagCooldown CONFIRM_COOLDOWN = new TagCooldown("mapmaker:play/reset_item_confirm", 2000);
    private static final int MIN_RESET_TIME = 1 * 60 * 1000; // 1 minute

    public static final String ID = "mapmaker:reset_savestate";
    public static final ResetSaveStateItem INSTANCE = new ResetSaveStateItem();

    private static final BadSprite SPRITE = Objects.requireNonNull(BadSprite.SPRITE_MAP.get("hud/hotbar/reset"));

    private ResetSaveStateItem() {
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
        if (world == null) return;

        // The player has no save state because they are spectating, so just re-add them to the world
        var saveState = SaveState.optionalFromPlayer(player);
        if (saveState == null) {
            FutureUtil.submitVirtual(() -> {
                world.removePlayer(player);
                world.addPlayer(player);
            });
            return;
        }

        if (requireConfirmation(player, saveState) && CONFIRM_COOLDOWN.test(player)) {
            player.sendMessage(Component.translatable("item.mapmaker.reset_savestate.confirm"));
            return;
        }

        if (world instanceof PlayingMapWorld || world instanceof TestingMapWorld) {
            EventDispatcher.call(new MapPlayerResetEvent(player, world, false));
        }
    }

    private boolean requireConfirmation(@NotNull Player player, @NotNull SaveState saveState) {
        // If you have a spectator checkpoint, never require confirmation
        if (player.hasTag(SPECTATOR_CHECKPOINT)) return false;
        // The reset item requires confirm click if the playtime is > MIN_RESET_TIME or if the player has a checkpoint
        if (saveState.getRealPlaytime() > MIN_RESET_TIME) return true;
        var playState = saveState.state(PlayState.class);
        // If you have a last state you have a checkpoint.
        return playState.lastState().isPresent();
    }

}
