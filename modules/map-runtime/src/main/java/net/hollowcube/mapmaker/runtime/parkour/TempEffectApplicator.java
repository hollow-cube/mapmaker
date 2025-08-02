package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.action.ActionList;
import net.hollowcube.mapmaker.map.action.Attachments;
import net.hollowcube.mapmaker.map.action.impl.SetProgressIndexAction;
import net.hollowcube.mapmaker.map.feature.play.effect.CheckpointEffectDataV2;
import net.hollowcube.mapmaker.map.feature.play.effect.StatusEffectData;
import net.hollowcube.mapmaker.map.util.MapMessages;
import net.hollowcube.mapmaker.map.world.savestate.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerStateUpdateEvent;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.kyori.adventure.text.Component;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.Map;

/// Temp while a bunch of pk stuff is still in the `map` module
/// Once it is moved to map runtime these can just live in their associated `data` classes.
public class TempEffectApplicator {
    private static final TagCooldown PROGRESS_INDEX_WARNING = new TagCooldown("mapmaker:play/progress_index_warning", 5000);

    // TODO: does this all need to be applied at a safe point or does it matter at all?
    public static void applyTo(CheckpointEffectDataV2 data, Player player, String checkpointId) {
        var world = ParkourMapWorld2.forPlayer(player);
        if (world == null) return;
        if (!(world.getPlayerState(player) instanceof ParkourState.Playing(var saveState, var _)))
            return;
        var playState = saveState.state(PlayState.class);

        // Ensure the event should trigger a checkpoint change for the current players state
        if (checkProgressIndex(player, world.map(), playState, data.actions())) return;
        if (playState.lastState() != null && playState.lastState().hasStatus(checkpointId))
            return; // Player already has this checkpoint in their history (they are backtracking)

        // Apply the checkpoint/effect changes
        world.callEvent(new ParkourMapPlayerStateUpdateEvent(world, player, saveState, playState));
        data.actions().applyTo(player, playState);

        // The checkpoint (reset) pos is set to the teleport if its present, or the first
        // position the player touched the checkpoint otherwise. todo probably need to do a gravity snap here
        // to bring it down to the ground.
        var checkpointPos = player.getPosition();

        List<String> newHistory;
        if (world.map().getSetting(MapSettings.PROGRESS_INDEX_ADDITION)) {
            // With additive progress index you can never touch a previous checkpoint
            playState.history().add(checkpointId);
            newHistory = List.copyOf(playState.history());
        } else {
            // Set the history to only contain the single checkpoint id so that you can go back to a previous
            // checkpoint though of course this can be prevented by using progress indices
            newHistory = List.of(checkpointId);
        }

        // Cache the last state so that we can reset back here.
        playState.setLastState(new PlayState(
                null,
                newHistory,
                checkpointPos,
                Map.copyOf(playState.ghostBlocks()),
                Map.copyOf(playState.actionData())
        ));

        // Update the player based on the new state
        world.callEvent(new ParkourMapPlayerUpdateStateEvent(world, player, saveState, playState, false));

        player.sendMessage(MapMessages.CHECKPOINT_REACHED);
    }

    public static void applyTo(StatusEffectData data, Player player, String statusId) {
        var world = ParkourMapWorld2.forPlayer(player);
        if (world == null) return;
        if (!(world.getPlayerState(player) instanceof ParkourState.Playing(var saveState, var _)))
            return;
        var playState = saveState.state(PlayState.class);

        // Ensure the event should trigger a status change for the current players state
        if (!data.repeatable() && playState.hasStatus(statusId))
            return; // Player already has the status plate in this checkpoint.
        if (checkProgressIndex(player, world.map(), playState, data.actions())) return;

        // Apply the status changes
        world.callEvent(new ParkourMapPlayerStateUpdateEvent(world, player, saveState, playState));
        data.actions().applyTo(player, playState);
        playState.addStatus(statusId);

        // Update the player based on the new state
        world.callEvent(new ParkourMapPlayerUpdateStateEvent(world, player, saveState, playState, false));
    }

    private static boolean checkProgressIndex(Player player, MapData map, PlayState state, ActionList actionList) {
        int progressIndex = OpUtils.mapOr(actionList.findLast(SetProgressIndexAction.class), SetProgressIndexAction::value, -1);
        if (progressIndex > 0) {
            int currentIndex = state.get(Attachments.PROGRESS_INDEX, 0);
            boolean condition = map.getSetting(MapSettings.PROGRESS_INDEX_ADDITION)
                    // With additive index you can get anything <= current + 1
                    ? (progressIndex > currentIndex + 1)
                    // Without additive progress index you must be at the prior index or the current one
                    : (progressIndex != currentIndex && progressIndex != currentIndex + 1);
            if (condition) {
                if (PROGRESS_INDEX_WARNING.test(player)) {
                    player.sendMessage(Component.translatable("checkpoint.progress_index.not_acceptable",
                            Component.text(currentIndex), Component.text(progressIndex - 1)));
                }
                return true;
            }
        }
        return false;
    }
}
