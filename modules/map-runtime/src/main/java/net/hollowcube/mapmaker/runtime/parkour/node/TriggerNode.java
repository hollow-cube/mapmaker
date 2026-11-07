package net.hollowcube.mapmaker.runtime.parkour.node;

import net.hollowcube.common.util.OpUtils;
import net.hollowcube.ipc.map.MapData;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.Trigger;
import net.hollowcube.mapmaker.map.util.debug.WireColor;
import net.hollowcube.mapmaker.runtime.PlayState;
import net.hollowcube.mapmaker.runtime.io.Connection;
import net.hollowcube.mapmaker.runtime.io.Dispatch;
import net.hollowcube.mapmaker.runtime.parkour.ParkourMapWorld;
import net.hollowcube.mapmaker.runtime.parkour.ParkourState;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerCondition;
import net.hollowcube.mapmaker.runtime.parkour.action.Attachments;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.SetProgressIndexAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.TeleportAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.variables.VariableQueries;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerStateUpdateEvent;
import net.hollowcube.mapmaker.runtime.parkour.event.ParkourMapPlayerUpdateStateEvent;
import net.hollowcube.molang.MolangState;
import net.hollowcube.molang.runtime.ContentError;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.minestom.server.coordinate.Point;
import net.minestom.server.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

import static net.kyori.adventure.text.Component.translatable;

/// A physical trigger with `enter` and `exit` connections: a holder's [TriggerNodeData] placed in the world.
///
/// @param id the legacy id, persisted in save state history and replays
public record TriggerNode(String id, Point position, TriggerNodeData data) implements Trigger {
    public static final Key ENTER = Key.key("mapmaker", "enter");
    public static final Key EXIT = Key.key("mapmaker", "exit");

    @Override
    public WireColor debugColor() {
        if (data.has(CheckpointPlayerInput.KEY)) return WireColor.WHITE;
        if (data.has(FinishPlayerInput.KEY)) return WireColor.YELLOW;
        if (data.has(ResetPlayerInput.KEY)) return WireColor.RED;
        return WireColor.LIGHT_GRAY;
    }

    boolean faceOnExit() {
        return data.legacy().faceOnExit() && !data.has(TeleportAction.KEY);
    }

    int progressIndex() {
        return OpUtils.mapOr(
            data.findLast(SetProgressIndexAction.class),
            SetProgressIndexAction::value,
            -1
        );
    }

    @Override
    public void onEnter(MapWorld ignored, Player player) {
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p)) return;
        var saveState = p.saveState();
        var playState = saveState.state(PlayState.class);
        var map = world.map();

        var legacy = data.legacy();
        if (data.once() && playState.hasStatus(id)) return;
        if (legacy.preCondition(player, map, playState, id, progressIndex())) return;
        if (checkCondition(player, map, playState, data.condition())) return;
        if (legacy.postCondition(player, map)) return;

        world.callEvent(new ParkourMapPlayerStateUpdateEvent(world, player, saveState, playState));
        var dispatch = new Dispatch(player, playState);
        dispatch.fire(new Dispatch.Source(id, position), ENTER, data.connections());
        dispatch.drain();
        if (replacesState(ENTER)) return;
        if (data.once()) playState.addStatus(id);
        world.callEvent(
            new ParkourMapPlayerUpdateStateEvent(
                world,
                player,
                saveState,
                playState,
                false,
                false,
                false
            )
        );
    }

    @Override
    public void onExit(MapWorld ignored, Player player) {
        boolean faceOnExit = faceOnExit();
        boolean hasExit = data.connections()
            .stream()
            .anyMatch(connection -> connection.output().equals(EXIT));
        if (!faceOnExit && !hasExit) return;
        var world = ParkourMapWorld.forPlayer(player);
        if (world == null) return;
        if (!(world.getPlayerState(player) instanceof ParkourState.AnyPlaying p)) return;
        var saveState = p.saveState();
        var playState = saveState.state(PlayState.class);

        if (faceOnExit)
            data.legacy().faceCheckpoint(player, playState, id, data.checkpointRespawn());
        if (!hasExit) return;

        world.callEvent(new ParkourMapPlayerStateUpdateEvent(world, player, saveState, playState));
        var dispatch = new Dispatch(player, playState);
        dispatch.fire(new Dispatch.Source(id, position), EXIT, data.connections());
        dispatch.drain();
        if (replacesState(EXIT)) return;
        world.callEvent(
            new ParkourMapPlayerUpdateStateEvent(
                world,
                player,
                saveState,
                playState,
                false,
                false,
                false
            )
        );
    }

    private boolean replacesState(Key output) {
        for (var connection : data.connections()) {
            if (connection.output().equals(output) && connection.params().replacesState())
                return true;
        }
        return false;
    }

    private static boolean checkCondition(
        Player player,
        MapData map,
        PlayState state,
        ActionTriggerCondition condition
    ) {
        var expression = condition.expression();
        if (expression == null) return false;
        if (expression.error() != null) return true;
        if (expression.program() == null) return true;

        List<ContentError> errors;
        boolean result = false;

        try {
            var molangState = new MolangState();
            result = expression.program().evalBool(
                molangState,
                new VariableQueries.Context(player, state.get(Attachments.VARIABLES))
            );
            errors = molangState.getErrors();
        } catch (Exception exception) {
            // Sanity check for unexpected errors, but molang should handle errors gracefully
            ExceptionReporter.reportException(exception, player);
            errors = List.of(
                new ContentError(
                    "Internal Server Error, please report to administrators if persistent."
                )
            );
        }

        if (!map.isPublished() && !errors.isEmpty()) {
            var error = errors.stream()
                .map(ContentError::toString)
                .collect(Collectors.joining("\n"));
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
}
