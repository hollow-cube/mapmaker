package net.hollowcube.mapmaker.runtime.parkour.datafix;

import com.google.auto.service.AutoService;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.datafix.DataVersion;
import net.hollowcube.datafix.ExternalDataFix;
import net.hollowcube.datafix.util.Value;

import java.util.List;

/// Converts legacy trigger data (`actions`, `repeatable`, `condition`) into trigger node data, turning checkpoint,
/// status and finish plates into `mapmaker:pressure_plate` blocks and checkpoint, status, finish and reset markers
/// into `mapmaker:trigger` markers.
///
/// Data the legacy codec rejected is carried over so that the node codec rejects it too: a malformed action stays a
/// malformed connection rather than being dropped.
@AutoService(ExternalDataFix.class)
public class V5025 extends DataVersion implements ExternalDataFix {
    private static final String ENTER = "mapmaker:enter";
    private static final List<String> NODE_KEYS = List.of("once", "legacy", "connections");
    private static final List<String> NODE_FIELDS = List.of(
        "once",
        "condition",
        "legacy",
        "connections"
    );
    private static final List<String> KIND_KEYS = List.of(
        "checkpoint",
        "status",
        "finish",
        "reset",
        "full_reset"
    );

    public V5025() {
        super(5025);

        // The three plate kinds become one pressure plate, which keeps the old handler id for its trigger id.
        addFix(DataTypes.BLOCK_ENTITY, "mapmaker:checkpoint_plate", plate -> {
            fixCheckpoint(plate);
            toTriggerPlate(plate, "mapmaker:checkpoint_plate");
            return null;
        });
        addFix(DataTypes.BLOCK_ENTITY, "mapmaker:status_plate", plate -> {
            fixStatus(plate);
            toTriggerPlate(plate, "mapmaker:status_plate");
            return null;
        });
        addFix(DataTypes.BLOCK_ENTITY, "mapmaker:finish_plate", plate -> {
            if (!isNode(plate))
                plate.put("connections", singleInput("mapmaker:finish").get("connections"));
            toTriggerPlate(plate, "mapmaker:finish_plate");
            return null;
        });

        // The four trigger marker kinds become one trigger marker, with its node at the root of the marker's data.
        addFix(DataTypes.ENTITY, "minecraft:marker", entity -> {
            var data = entity.get("data");
            if (!data.isMapLike()) return null;

            Value node;
            switch (data.get("type").as(String.class, "")) {
                case "mapmaker:checkpoint" -> {
                    node = kindData(data, "checkpoint");
                    fixCheckpoint(node);
                }
                case "mapmaker:status" -> {
                    node = kindData(data, "status");
                    fixStatus(node);
                }
                case "mapmaker:finish" -> node = singleInput("mapmaker:finish");
                case "mapmaker:reset" -> {
                    node = singleInput("mapmaker:reset");
                    // Read as NBT's getBoolean did: any non-zero number, anything else false.
                    var hard = data.get("full_reset").value();
                    node.get("connections")
                        .get(0)
                        .put(
                            "hard",
                            hard instanceof Number n
                                ? n.byteValue() != 0
                                : Boolean.TRUE.equals(hard)
                        );
                }
                default -> {
                    return null;
                }
            }

            for (var key : KIND_KEYS) data.remove(key);
            for (var key : NODE_FIELDS) data.remove(key);
            for (var key : NODE_FIELDS) data.put(key, node.get(key));
            data.put("type", "mapmaker:trigger");
            return null;
        });
    }

    private static void toTriggerPlate(Value plate, String legacyId) {
        plate.put("legacy_id", legacyId);
        plate.put("id", "mapmaker:pressure_plate");
    }

    /// The legacy data for a marker's kind. A missing or non-compound value read as empty legacy data.
    private static Value kindData(Value data, String key) {
        var value = data.get(key);
        return value.isMapLike() ? value : Value.emptyMap();
    }

    private static Value singleInput(String input) {
        var connection = Value.emptyMap();
        connection.put("output", ENTER);
        connection.put("input", input);
        var connections = Value.emptyList();
        connections.put(connection);
        var node = Value.emptyMap();
        node.put("connections", connections);
        return node;
    }

    private static void fixCheckpoint(Value trigger) {
        if (isNode(trigger)) return;
        var connections = convertTrigger(trigger);
        // Checkpoints never read `repeatable`.
        trigger.remove("repeatable");

        var checkpoint = Value.emptyMap();
        checkpoint.put("output", ENTER);
        checkpoint.put("input", "mapmaker:checkpoint");
        if (connections.isListLike()) {
            // The checkpoint used the last `respawn_position` action, which did nothing else, so it moves onto the
            // checkpoint and the others go.
            var kept = Value.emptyList();
            for (var connection : connections) {
                if (connection.isMapLike()
                    && "mapmaker:respawn_position".equals(connection.get("input").value())) {
                    var respawn = Value.emptyMap();
                    connection.forEachEntry((key, value) -> {
                        if (!key.equals("output") && !key.equals("input")) respawn.put(key, value);
                    });
                    checkpoint.put("respawn", respawn);
                } else {
                    kept.put(connection);
                }
            }
            kept.put(checkpoint);
            trigger.put("connections", kept);
        }

        var legacy = Value.emptyMap();
        legacy.put("no_backtrack", true);
        legacy.put("face_on_exit", true);
        trigger.put("legacy", legacy);
    }

    private static void fixStatus(Value trigger) {
        if (isNode(trigger)) return;
        convertTrigger(trigger);

        var repeatable = trigger.remove("repeatable");
        var value = repeatable.value();
        if (value == null) trigger.put("once", true);
        else if (value instanceof Number n) trigger.put("once", n.byteValue() == 0);
        else if (value instanceof Boolean b) trigger.put("once", !b);
        else trigger.put("once", repeatable);

        var legacy = Value.emptyMap();
        legacy.put("status_cooldown", true);
        trigger.put("legacy", legacy);
    }

    /// Node data can come through again when something labels it with an older version, as schematics written with
    /// the game's data version did.
    private static boolean isNode(Value trigger) {
        return !trigger.get("connections").isNull() && trigger.get("actions").isNull();
    }

    /// Moves `actions` into `connections` and returns them.
    private static Value convertTrigger(Value trigger) {
        // Legacy decoding ignored unknown keys, so stray ones with the node's names must not start meaning something.
        for (var key : NODE_KEYS) trigger.remove(key);

        var actions = trigger.remove("actions");
        Value connections;
        if (actions.isNull()) {
            connections = Value.emptyList();
        } else if (actions.isListLike()) {
            connections = Value.emptyList();
            for (var action : actions) connections.put(toConnection(action));
        } else {
            connections = actions;
        }
        trigger.put("connections", connections);
        return connections;
    }

    private static Value toConnection(Value action) {
        if (!action.isMapLike()) return action;

        var connection = Value.emptyMap();
        action.forEachEntry((key, value) -> {
            if (!key.equals("type")) connection.put(key, value);
        });
        connection.put("output", ENTER);
        connection.put("input", action.get("type"));
        return connection;
    }
}
