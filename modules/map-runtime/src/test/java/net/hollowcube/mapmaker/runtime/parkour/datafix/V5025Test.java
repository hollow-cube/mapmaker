package net.hollowcube.mapmaker.runtime.parkour.datafix;

import net.hollowcube.common.math.relative.RelativePos;
import net.hollowcube.datafix.DataFixer;
import net.hollowcube.datafix.DataTypes;
import net.hollowcube.mapmaker.runtime.io.Connection;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerCondition;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.action.MolangExpression;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.ChatAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.RespawnPosAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.TeleportAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.variables.VariableQueries;
import net.hollowcube.mapmaker.runtime.parkour.node.CheckpointPlayerInput;
import net.hollowcube.mapmaker.runtime.parkour.node.LegacyRules;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerNodeData;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerTemplate;
import net.kyori.adventure.nbt.BinaryTag;
import net.kyori.adventure.nbt.BinaryTagTypes;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.kyori.adventure.nbt.ListBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.codec.Result;
import net.minestom.server.codec.Transcoder;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.registry.RegistryTranscoder;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class V5025Test {
    private static final List<Action> ACTIONS = List.of(
        new ChatAction("a"),
        new TeleportAction(RelativePos.ORIGIN)
    );
    private static final ActionTriggerCondition CONDITION = new ActionTriggerCondition(
        MolangExpression.from(VariableQueries.ENVIRONMENT, "q.playtime > 1"),
        false,
        "nope"
    );

    private static Transcoder<BinaryTag> coder;

    @BeforeAll
    static void setup() {
        MinecraftServer.init();
        DataFixer.buildModel();
        coder = new RegistryTranscoder<>(Transcoder.NBT, MinecraftServer.process());
    }

    @Test
    void checkpointPlateKeepsActionsAndAddsTheCheckpoint() {
        var node = decode(plate("mapmaker:checkpoint_plate", legacy(true)));

        assertFalse(node.once());
        assertEquals(new LegacyRules(true, false, true), node.legacy());
        assertCondition(node.condition());
        assertEquals(
            ACTIONS,
            node.connections().stream().limit(2).map(Connection::params).toList()
        );
        assertInstanceOf(CheckpointPlayerInput.class, node.connections().getLast().params());
        assertEquals(3, node.connections().size());
    }

    @Test
    void platesBecomePressurePlatesKeepingTheirId() {
        for (var id : List.of(
            "mapmaker:checkpoint_plate",
            "mapmaker:status_plate",
            "mapmaker:finish_plate"
        )) {
            var fixed = plate(id, CompoundBinaryTag.empty());
            assertEquals("mapmaker:pressure_plate", fixed.getString("id"));
            assertEquals(id, fixed.getString("legacy_id"));
        }
    }

    @Test
    void platesWithoutDataGetTheirKindsPreset() {
        assertEquals(
            TriggerTemplate.checkpoint(),
            decode(plate("mapmaker:checkpoint_plate", CompoundBinaryTag.empty()))
        );
        assertEquals(
            TriggerTemplate.status(),
            decode(plate("mapmaker:status_plate", CompoundBinaryTag.empty()))
        );
        assertEquals(
            TriggerTemplate.finish(),
            decode(plate("mapmaker:finish_plate", CompoundBinaryTag.empty()))
        );
    }

    @Test
    void checkpointRespawnMovesOntoTheCheckpoint() {
        var last = new RespawnPosAction(RelativePos.abs(new Pos(1, 2, 3)));
        List<Action> actions = List.of(
            new RespawnPosAction(RelativePos.ORIGIN),
            new ChatAction("a"),
            last
        );
        var data = new ActionTriggerData(
            new ActionList(actions),
            false,
            ActionTriggerCondition.DEFAULT
        );
        var legacy = (CompoundBinaryTag) ActionTriggerData.CODEC.encode(coder, data).orElseThrow();

        var node = decode(plate("mapmaker:checkpoint_plate", legacy));

        assertEquals(
            List.of(new ChatAction("a"), new CheckpointPlayerInput(last.target(), false)),
            node.connections().stream().map(Connection::params).toList()
        );
    }

    @Test
    void statusPlateOnceIsNotRepeatable() {
        var once = decode(plate("mapmaker:status_plate", legacy(false)));
        var repeatable = decode(plate("mapmaker:status_plate", legacy(true)));
        var unset = decode(plate("mapmaker:status_plate", legacy(false).remove("repeatable")));

        assertTrue(once.once());
        assertFalse(repeatable.once());
        assertTrue(unset.once());
        assertEquals(new LegacyRules(false, true, false), once.legacy());
        assertEquals(ACTIONS, once.connections().stream().map(Connection::params).toList());
    }

    @Test
    void roundTripsThroughTheEditorView() {
        var node = decode(plate("mapmaker:status_plate", legacy(true)));
        var data = node.actionData();

        assertTrue(data.repeatable());
        assertCondition(data.condition());
        assertEquals(ACTIONS, data.actions().actions());
        assertEquals(node, node.withActionData(data));
    }

    @Test
    void checkpointMarkerBecomesATriggerMarker() {
        var data = marker("mapmaker:checkpoint")
            .put("checkpoint", legacy(false))
            .put("status", legacy(false))
            .putString("name", "start")
            .build();

        var fixed = upgradeMarker(data);

        assertEquals("mapmaker:trigger", fixed.getString("type"));
        assertEquals("start", fixed.getString("name"));
        assertFalse(fixed.keySet().contains("checkpoint"));
        assertFalse(fixed.keySet().contains("status"));
        assertEquals(3, decode(fixed).connections().size());
        assertInstanceOf(
            CheckpointPlayerInput.class,
            decode(fixed).connections().getLast().params()
        );
    }

    @Test
    void statusMarkerWithoutDataIsTheStatusPreset() {
        var fixed = upgradeMarker(marker("mapmaker:status").build());

        assertEquals(TriggerTemplate.status(), decode(fixed));
    }

    @Test
    void finishAndResetMarkersBecomeTriggerMarkers() {
        var finish = decode(upgradeMarker(marker("mapmaker:finish").build()));
        var soft = decode(upgradeMarker(marker("mapmaker:reset").build()));
        var hard = decode(
            upgradeMarker(marker("mapmaker:reset").putBoolean("full_reset", true).build())
        );

        assertEquals(TriggerTemplate.finish(), finish);
        assertEquals(TriggerTemplate.reset(false), soft);
        assertEquals(TriggerTemplate.reset(true), hard);
    }

    @Test
    void otherMarkersAreLeftAlone() {
        var data = marker("mapmaker:map_leaderboard").put("checkpoint", legacy(false)).build();

        assertEquals(data, upgradeMarker(data));
    }

    @Test
    void malformedActionsStillFail() {
        var noType = legacy(
            false
        ).put(
            "actions",
            ListBinaryTag.listBinaryTag(BinaryTagTypes.COMPOUND, List.of(CompoundBinaryTag.empty()))
        );
        var fixed = upgrade(DataTypes.BLOCK_ENTITY, withId("mapmaker:checkpoint_plate", noType));

        assertInstanceOf(Result.Error.class, TriggerNodeData.CODEC.decode(coder, fixed));
    }

    @Test
    void strayNodeKeysAreDropped() {
        var stray = legacy(false).putBoolean("once", false);
        var node = decode(plate("mapmaker:status_plate", stray));

        assertTrue(node.once());
    }

    @Test
    void convertedDataIsLeftAlone() {
        var once = plate("mapmaker:checkpoint_plate", legacy(false));
        var twice = upgrade(
            DataTypes.BLOCK_ENTITY,
            withId("mapmaker:checkpoint_plate", once.remove("id"))
        );

        assertEquals(once, twice);
    }

    private static void assertCondition(ActionTriggerCondition condition) {
        assertEquals(CONDITION.expression().text(), condition.expression().text());
        assertEquals(CONDITION.showMessage(), condition.showMessage());
        assertEquals(CONDITION.message(), condition.message());
    }

    private static CompoundBinaryTag.Builder marker(String type) {
        return CompoundBinaryTag.builder().putString("type", type);
    }

    private static CompoundBinaryTag upgradeMarker(CompoundBinaryTag data) {
        var entity = CompoundBinaryTag.builder()
            .putString("id", "minecraft:marker")
            .put("data", data)
            .build();
        return upgrade(DataTypes.ENTITY, entity).getCompound("data");
    }

    private static CompoundBinaryTag legacy(boolean repeatable) {
        var data = new ActionTriggerData(new ActionList(ACTIONS), repeatable, CONDITION);
        return (CompoundBinaryTag) ActionTriggerData.CODEC.encode(coder, data).orElseThrow();
    }

    private static CompoundBinaryTag plate(String id, CompoundBinaryTag data) {
        return upgrade(DataTypes.BLOCK_ENTITY, withId(id, data));
    }

    private static CompoundBinaryTag withId(String id, CompoundBinaryTag data) {
        return CompoundBinaryTag.builder().putString("id", id).put(data).build();
    }

    private static CompoundBinaryTag upgrade(
        net.hollowcube.datafix.DataType type,
        CompoundBinaryTag tag
    ) {
        return (CompoundBinaryTag) DataFixer.upgrade(type, Transcoder.NBT, tag, 5024, 5025);
    }

    private static TriggerNodeData decode(CompoundBinaryTag tag) {
        return TriggerNodeData.CODEC.decode(coder, tag).orElseThrow();
    }
}
