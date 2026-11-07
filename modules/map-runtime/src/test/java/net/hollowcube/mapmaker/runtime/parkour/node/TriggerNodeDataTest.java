package net.hollowcube.mapmaker.runtime.parkour.node;

import net.hollowcube.common.math.relative.RelativePos;
import net.hollowcube.mapmaker.runtime.io.Connection;
import net.hollowcube.mapmaker.runtime.parkour.action.Action;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerCondition;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.ChatAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.RespawnPosAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.SetProgressIndexAction;
import net.hollowcube.mapmaker.runtime.parkour.action.impl.TeleportAction;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.tag.TagHandler;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TriggerNodeDataTest {
    private static final BlockVec POS = new BlockVec(1, 2, 3);

    @BeforeAll
    static void setup() {
        MinecraftServer.init();
    }

    @Test
    void checkpointKeepsEveryActionThenSetsTheCheckpoint() {
        var lastRespawn = new RespawnPosAction(RelativePos.abs(new Pos(9, 9, 9)));
        List<Action> actions = List.of(
            new ChatAction("a"),
            new RespawnPosAction(RelativePos.ORIGIN),
            new SetProgressIndexAction(1),
            new SetProgressIndexAction(4),
            lastRespawn,
            new ChatAction("b")
        );
        var node = new TriggerNode("cp", POS, withActions(TriggerTemplate.checkpoint(), actions));

        var stored = actions.stream()
            .filter(action -> !(action instanceof RespawnPosAction))
            .toList();
        assertEquals(stored.size() + 1, node.data().connections().size());
        assertTrue(
            node.data().connections().stream().allMatch(c -> c.output().equals(TriggerNode.ENTER))
        );
        for (int i = 0; i < stored.size(); i++)
            assertSame(stored.get(i), node.data().connections().get(i).params());
        var checkpoint = assertInstanceOf(
            CheckpointPlayerInput.class,
            node.data().connections().getLast().params()
        );
        assertSame(lastRespawn.target(), checkpoint.respawn());
        assertFalse(checkpoint.temporary());
        assertEquals(lastRespawn, node.data().actionData().actions().actions().getLast());

        assertFalse(node.data().once());
        assertEquals(new LegacyRules(true, false, true), node.data().legacy());
        assertEquals(4, node.progressIndex());
        assertTrue(node.faceOnExit());
    }

    @Test
    void teleportingCheckpointDoesNotFaceOnExit() {
        var node = new TriggerNode(
            "cp",
            POS,
            withActions(
                TriggerTemplate.checkpoint(),
                List.of(new TeleportAction(RelativePos.ORIGIN))
            )
        );

        assertFalse(node.faceOnExit());
        assertNull(
            assertInstanceOf(
                CheckpointPlayerInput.class,
                node.data().connections().getLast().params()
            ).respawn()
        );
    }

    @Test
    void statusIsOnlyItsActions() {
        var node = new TriggerNode(
            "st",
            POS,
            withActions(TriggerTemplate.status(), List.of(new ChatAction("a")))
        );

        assertEquals(
            List.of(ChatAction.KEY),
            node.data().connections().stream().map(Connection::input).toList()
        );
        assertTrue(node.data().once());
        assertEquals(new LegacyRules(false, true, false), node.data().legacy());
        assertEquals(-1, node.progressIndex());
    }

    @Test
    void repeatableStatusIsNotOnce() {
        var status = TriggerTemplate.status();
        var data = status.actionData();
        var repeatable = new ActionTriggerData(data.actions(), true, data.condition());
        assertFalse(status.withActionData(repeatable).once());
    }

    @Test
    void finishOnlyFinishes() {
        var finish = new TriggerNode("f", POS, TriggerTemplate.finish());

        assertInstanceOf(FinishPlayerInput.class, finish.data().connections().getFirst().params());
        assertEquals(1, finish.data().connections().size());
        assertEquals(LegacyRules.NONE, finish.data().legacy());
        assertFalse(finish.data().once());
    }

    @Test
    void resetOnlyResets() {
        var node = new TriggerNode("r", POS, TriggerTemplate.reset(true));

        assertTrue(
            assertInstanceOf(
                ResetPlayerInput.class,
                node.data().connections().getFirst().params()
            ).hard()
        );
        assertEquals(1, node.data().connections().size());
        assertEquals(LegacyRules.NONE, node.data().legacy());
    }

    @Test
    void editorKeepsConnectionsItCantShow() {
        var reset = new Connection(
            TriggerNode.ENTER,
            ResetPlayerInput.KEY,
            new ResetPlayerInput(true)
        );
        var exit = new Connection(TriggerNode.EXIT, ChatAction.KEY, new ChatAction("bye"));
        var node = new TriggerNodeData(
            true,
            ActionTriggerCondition.DEFAULT,
            LegacyRules.NONE,
            List.of(
                reset,
                new Connection(TriggerNode.ENTER, ChatAction.KEY, new ChatAction("a")),
                exit
            )
        );

        var data = node.actionData();
        assertEquals(List.of(new ChatAction("a")), data.actions().actions());
        var edited = withActions(node, List.of(new ChatAction("b"), new ChatAction("c")));

        assertEquals(
            List.of(
                new ResetPlayerInput(true),
                new ChatAction("b"),
                new ChatAction("c"),
                new ChatAction("bye")
            ),
            edited.connections().stream().map(Connection::params).toList()
        );
        assertEquals(exit, edited.connections().getLast());
        assertEquals(
            List.of(new ResetPlayerInput(true), new ChatAction("bye")),
            withActions(node, List.of()).connections().stream().map(Connection::params).toList()
        );
    }

    @Test
    void actionsAddedToACheckpointRunBeforeIt() {
        var edited = withActions(TriggerTemplate.checkpoint(), List.of(new ChatAction("a")));

        assertInstanceOf(ChatAction.class, edited.connections().getFirst().params());
        assertInstanceOf(CheckpointPlayerInput.class, edited.connections().getLast().params());
    }

    @Test
    void markerWithoutNodeDataIsEmpty() {
        var tag = TriggerNodeData.TAG.path("data");
        for (var data : List.of(
            CompoundBinaryTag.empty(),
            CompoundBinaryTag.builder().putString("type", "mapmaker:trigger").build()
        )) {
            var holder = TagHandler.fromCompound(
                CompoundBinaryTag.builder().put("data", data).build()
            );
            assertEquals(TriggerNodeData.EMPTY, holder.getTag(tag));
        }
    }

    @Test
    void mergingKeepsTheMarkersOtherData() {
        var data = CompoundBinaryTag.builder()
            .putString("type", "mapmaker:trigger")
            .putString("name", "start")
            .putBoolean("once", true)
            .build();

        var merged = TriggerTemplate.finish().mergeInto(data);

        assertEquals("mapmaker:trigger", merged.getString("type"));
        assertEquals("start", merged.getString("name"));
        assertEquals(
            TriggerTemplate.finish(),
            TagHandler.fromCompound(merged).getTag(TriggerNodeData.TAG)
        );
    }

    @Test
    void missingNodeDataIsEmpty() {
        assertEquals(TriggerNodeData.EMPTY, TagHandler.newHandler().getTag(TriggerNodeData.TAG));
    }

    private static TriggerNodeData withActions(TriggerNodeData node, List<Action> actions) {
        var data = node.actionData();
        return node.withActionData(
            new ActionTriggerData(new ActionList(actions), data.repeatable(), data.condition())
        );
    }
}
