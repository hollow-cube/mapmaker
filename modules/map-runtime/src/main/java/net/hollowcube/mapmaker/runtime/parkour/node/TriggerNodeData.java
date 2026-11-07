package net.hollowcube.mapmaker.runtime.parkour.node;

import net.hollowcube.common.math.relative.RelativePos;
import net.hollowcube.common.util.dfu.DFU;
import net.hollowcube.mapmaker.runtime.io.Connection;
import net.hollowcube.mapmaker.runtime.io.PlayerInput;
import net.hollowcube.mapmaker.runtime.parkour.action.*;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.RespawnPosAction;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.codec.Codec;
import net.minestom.server.codec.StructCodec;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record TriggerNodeData(
    boolean once,
    ActionTriggerCondition condition,
    LegacyRules legacy,
    List<Connection> connections
) {
    private static final Map<Key, StructCodec<? extends PlayerInput>> NODE_INPUTS = Map.of(
        CheckpointPlayerInput.KEY, CheckpointPlayerInput.CODEC,
        FinishPlayerInput.KEY, FinishPlayerInput.CODEC,
        ResetPlayerInput.KEY, ResetPlayerInput.CODEC
    );
    private static final StructCodec<PlayerInput> INPUT_CODEC = Codec.KEY.unionType(
        "input",
        TriggerNodeData::inputCodec,
        TriggerNodeData::inputKey
    );
    public static final StructCodec<Connection> CONNECTION_CODEC = StructCodec.struct(
        "output", Codec.KEY, Connection::output,
        StructCodec.INLINE, INPUT_CODEC, Connection::params,
        (output, params) -> new Connection(output, inputKey(params), params)
    );
    public static final StructCodec<TriggerNodeData> CODEC = StructCodec.struct(
        "once", Codec.BOOLEAN.optional(false), TriggerNodeData::once,
        "condition", ActionTriggerCondition.CODEC.optional(ActionTriggerCondition.DEFAULT), TriggerNodeData::condition,
        "legacy", LegacyRules.CODEC.optional(LegacyRules.NONE), TriggerNodeData::legacy,
        "connections", CONNECTION_CODEC.list().optional(List.of()), TriggerNodeData::connections,
        TriggerNodeData::new
    );

    public static final TriggerNodeData EMPTY = new TriggerNodeData(
        false,
        ActionTriggerCondition.DEFAULT,
        LegacyRules.NONE,
        List.of()
    );

    public TriggerNodeData {
        connections = List.copyOf(connections);
    }

    /// `compound` with this node's fields replaced, keeping the holder's own keys (a marker's `type`, `name` and
    /// region, a plate's `legacy_id`), which writing [#TAG] would drop.
    public CompoundBinaryTag mergeInto(CompoundBinaryTag compound) {
        var merged = CompoundBinaryTag.builder().put(compound);
        for (var field : FIELDS) merged.remove(field);
        return merged.put(DFU.encodeNbt(CODEC, this)).build();
    }

    /// The trigger as the action list editor shows it. Connections it can't show (the checkpoint, finish and reset
    /// inputs, anything not on [TriggerNode#ENTER]) are left out, and [#withActionData] keeps them. A checkpoint's
    /// respawn is shown as the `respawn_position` action it was before it moved onto the checkpoint.
    public ActionTriggerData actionData() {
        var actions = new ArrayList<>(actions().actions());
        var respawn = checkpointRespawn();
        if (respawn != null) actions.add(new RespawnPosAction(respawn));
        return new ActionTriggerData(new ActionList(actions), !once, condition);
    }

    /// Replaces the actions on [TriggerNode#ENTER] with `data`'s, in its order. The connections the editor doesn't
    /// show keep their positions: actions fill the existing action slots in turn, and any extra go after the last of
    /// them (or first, if there were none, so they still run before a checkpoint). On a checkpoint, a
    /// `respawn_position` action sets the checkpoint's respawn rather than becoming a connection.
    public TriggerNodeData withActionData(ActionTriggerData data) {
        var edited = new ArrayList<>(data.actions().actions());
        var current = connections;
        if (has(CheckpointPlayerInput.KEY)) {
            RelativePos respawn = null;
            for (var action : edited)
                if (action instanceof RespawnPosAction(var target)) respawn = target;
            edited.removeIf(RespawnPosAction.class::isInstance);
            current = withCheckpointRespawn(respawn);
        }

        var actions = new ActionList(edited).connections(TriggerNode.ENTER).iterator();
        var updated = new ArrayList<Connection>(current.size());
        int insertAt = 0;
        for (var connection : current) {
            if (!isAction(connection)) {
                updated.add(connection);
            } else if (actions.hasNext()) {
                updated.add(actions.next());
                insertAt = updated.size();
            }
        }
        while (actions.hasNext()) updated.add(insertAt++, actions.next());
        return new TriggerNodeData(!data.repeatable(), data.condition(), legacy, updated);
    }

    /// Where this trigger's checkpoint respawns the player, relative to the trigger, or null for where they stand.
    public @Nullable RelativePos checkpointRespawn() {
        for (var connection : connections) {
            if (connection.params() instanceof CheckpointPlayerInput checkpoint)
                return checkpoint.respawn();
        }
        return null;
    }

    private List<Connection> withCheckpointRespawn(@Nullable RelativePos respawn) {
        var updated = new ArrayList<Connection>(connections.size());
        for (var connection : connections) {
            if (connection.params() instanceof CheckpointPlayerInput(var _, var temporary)) {
                var checkpoint = new CheckpointPlayerInput(respawn, temporary);
                updated.add(new Connection(connection.output(), connection.input(), checkpoint));
            } else {
                updated.add(connection);
            }
        }
        return updated;
    }

    public <T extends Action> @Nullable T findLast(Class<T> type) {
        return actions().findLast(type);
    }

    public boolean has(Key input) {
        return connections.stream().anyMatch(connection -> connection.input().equals(input));
    }

    private ActionList actions() {
        var actions = new ArrayList<Action>();
        for (var connection : connections) {
            if (isAction(connection)) actions.add((Action) connection.params());
        }
        return new ActionList(actions);
    }

    private static boolean isAction(Connection connection) {
        return connection.output().equals(TriggerNode.ENTER)
            && connection.params() instanceof Action;
    }

    private static @Nullable StructCodec<? extends PlayerInput> inputCodec(Key key) {
        var nodeInput = NODE_INPUTS.get(key);
        return nodeInput != null ? nodeInput : ActionRegistry.codec(key);
    }

    // TODO: needs some kind of registry later
    static Key inputKey(PlayerInput input) {
        return switch (input) {
            case Action action -> ActionRegistry.getKey(action);
            case CheckpointPlayerInput _ -> CheckpointPlayerInput.KEY;
            case FinishPlayerInput _ -> FinishPlayerInput.KEY;
            case ResetPlayerInput _ -> ResetPlayerInput.KEY;
            default -> throw new IllegalArgumentException("not a stored input: " + input);
        };
    }

    private static final List<String> FIELDS = List.of(
        "once",
        "condition",
        "legacy",
        "connections"
    );

    /// The holder's root compound as node data; on a marker, `path("data")` it.
    public static final Tag<TriggerNodeData> TAG = DFU.View(CODEC);
}
