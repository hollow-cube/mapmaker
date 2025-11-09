package net.hollowcube.mapmaker.runtime.parkour;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapData;
import net.hollowcube.mapmaker.map.MapSettings;
import net.hollowcube.mapmaker.map.SaveState;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerCondition;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.RespawnPosAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.SetProgressIndexAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.TeleportAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.variables.VariableStorage;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerStateUpdateEvent;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.hollowcube.mapmaker.util.TagCooldown;
import net.hollowcube.molang.eval.MolangEvaluator;
import net.hollowcube.molang.runtime.ContentError;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Point;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.sound.SoundEvent;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.translatable;

/// Temp while a bunch of pk stuff is still in the `map` module
/// Once it is moved to map runtime these can just live in their associated `data` classes.
public class TempEffectApplicator {
    private static final TagCooldown PROGRESS_INDEX_WARNING = new TagCooldown("mapmaker:play/progress_index_warning", 5000);

    private static final Sound CHECKPOINT_SOUND = Sound.sound(SoundEvent.ENTITY_EXPERIENCE_ORB_PICKUP, Sound.Source.MASTER, 0.1f, 0f);
    private static final TagCooldown STATUS_APPLY_COOLDOWN = new TagCooldown("mapmaker:status_plate_cooldown", 250);

    private static final VariableStorage.MolangLookup VARIABLE_LOOKUP = VariableStorage.lookup();
    private static final MolangEvaluator EVALUATOR = new MolangEvaluator(Map.of(
            "variable", VARIABLE_LOOKUP,
            "v", VARIABLE_LOOKUP
    ));

    public static void applyCheckpoint(ActionTriggerData data, Player player, String checkpointId, Point position) {
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return;

        applyCheckpoint(world, data, player, p.saveState(), checkpointId, position, false);
    }

    // TODO: does this all need to be applied at a safe point or does it matter at all?
    public static void applyCheckpoint(ParkourMapWorld world, ActionTriggerData data, Player player, SaveState saveState, String checkpointId, Point position, boolean isTemporary) {
        var playState = saveState.state(PlayState.class);

        // Ensure the event should trigger a checkpoint change for the current players state
        if (checkProgressIndex(player, world.map(), playState, data.actions())) return;
        if (playState.lastState() != null && playState.lastState().hasStatus(checkpointId))
            return; // Player already has this checkpoint in their history (they are backtracking)
        if (checkCondition(player, world.map(), playState, data.condition())) return;

        // Apply the checkpoint/effect changes
        world.callEvent(new ParkourMapPlayerStateUpdateEvent(world, player, saveState, playState));
        data.actions().applyTo(player, playState);

        // The checkpoint (reset) pos is set to the respawn position if its present, or if
        // its present the teleport position, or the first
        // position the player touched the checkpoint otherwise.
        var respawnPosition = OpUtils.mapOr(
                data.actions().findLast(RespawnPosAction.class),
                action -> {
                    float yaw = player.getPosition().yaw();
                    float pitch = player.getPosition().pitch();
                    Pos origin;
                    if (position instanceof BlockVec(int x, int y, int z)) {
                        // If the position is a block vector then we want to center the player on the block
                        origin = new Pos(x + 0.5, y, z + 0.5, yaw, pitch);
                    } else {
                        origin = new Pos(position, yaw, pitch);
                    }
                    return action.target().resolve(origin);
                },
                player.getPosition()
        );

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
                respawnPosition,
                Map.copyOf(playState.ghostBlocks()),
                Map.copyOf(playState.actionData())
        ));

        // Update the player based on the new state
        world.callEvent(new ParkourMapPlayerUpdateStateEvent(world, player, saveState, playState, false, false, false));

        player.sendMessage(translatable(isTemporary ? "spec.checkpoint.set" : "play.checkpoint.reached"));
        player.playSound(CHECKPOINT_SOUND);
    }

    public static void applyStatus(ActionTriggerData data, Player player, String statusId) {
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return;
        var saveState = p.saveState();
        var playState = saveState.state(PlayState.class);

        // Ensure the event should trigger a status change for the current players state
        if (!data.repeatable() && playState.hasStatus(statusId))
            return; // Player already has the status plate in this checkpoint.
        if (checkProgressIndex(player, world.map(), playState, data.actions())) return;
        if (checkCondition(player, world.map(), playState, data.condition())) return;
        if (!STATUS_APPLY_COOLDOWN.test(player)) return;

        // Update the play state from the player's current view of the world.
        world.callEvent(new ParkourMapPlayerStateUpdateEvent(world, player, saveState, playState));

        // Apply the status changes
        data.actions().applyTo(player, playState);
        playState.addStatus(statusId);

        // Update the player based on the new state
        world.callEvent(new ParkourMapPlayerUpdateStateEvent(world, player, saveState, playState, false, false, false));
    }

    public static void handleCheckpointExit(Player player, ActionTriggerData data, String checkpointId) {
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p))
            return;
        var saveState = p.saveState();
        var state = saveState.state(PlayState.class);

        if (state.history().isEmpty()) return;
        if (state.lastState() == null) return;
        if (!state.history().getLast().equals(checkpointId)) return;
        if (data.actions().has(TeleportAction.KEY)) return;
        var pos = state.pos();
        if (pos == null) return;
        float yaw = player.getPosition().yaw();
        float pitch = player.getPosition().pitch();

        state.setPos(pos.withView(yaw, pitch));
        if (state.lastState() != null) {
            var lastPos = state.lastState().pos();
            if (lastPos != null) {
                state.lastState().setPos(lastPos.withView(yaw, pitch));
            }
        }
    }

    private static boolean checkCondition(Player player, MapData map, PlayState state, ActionTriggerCondition condition) {
        var expression = condition.expression();
        if (expression == null) return false;
        if (expression.error() != null) return true;
        if (expression.parsed() == null) return true;

        List<ContentError> errors;
        boolean result = false;

        try {
            VARIABLE_LOOKUP.setStorage(state.get(Attachments.VARIABLES));
            result = EVALUATOR.evalBool(expression.parsed());
            errors = EVALUATOR.getErrors();
        } catch (ArithmeticException exception) {
            errors = List.of(new ContentError(exception.getMessage()));
        } catch (Exception exception) {
            // Sanity check for unexpected errors, but molang should handle errors gracefully
            ExceptionReporter.reportException(exception, player);
            errors = List.of(new ContentError("Internal Server Error, please report to administrators if persistent."));
        }

        if (!map.isPublished() && !errors.isEmpty()) {
            var error = errors.stream().map(ContentError::message).collect(Collectors.joining("\n"));
            player.sendMessage(Component.text("Errors evaluating condition:\n" + error));
        }

        if (result) return false;

        if (condition.showMessage()) {
            Component message;
            if (condition.message().isBlank()) {
                message = translatable("action.condition.not_acceptable");
            } else {
                message = translatable(
                        "action.condition.not_acceptable.custom",
                        List.of(Component.text(condition.message()))
                );
            }
            player.sendMessage(message);
        }

        return true;
    }

    private static boolean checkProgressIndex(Player player, MapData map, PlayState state, ActionList actionList) {
        int progressIndex = OpUtils.mapOr(actionList.findLast(SetProgressIndexAction.class), SetProgressIndexAction::value, -1);
        if (progressIndex > 0) {
            int currentIndex = state.get(Attachments.PROGRESS_INDEX, 0);
            boolean isFail = map.getSetting(MapSettings.PROGRESS_INDEX_ADDITION)
                    // With additive index you can get anything <= current + 1
                    ? (progressIndex > currentIndex + 1)
                    // Without additive progress index you must be at the prior index or the current one
                    : (progressIndex - 1 > currentIndex);
            if (isFail) {
                if (PROGRESS_INDEX_WARNING.test(player)) {
                    player.sendMessage(translatable("action.progress_index.not_acceptable",
                            Component.text(currentIndex), Component.text(progressIndex - 1)));
                }
                return true;
            }
        }
        return false;
    }
}
