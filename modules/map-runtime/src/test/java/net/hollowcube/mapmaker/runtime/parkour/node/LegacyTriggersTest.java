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
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LegacyTriggersTest {
    private static final BlockVec POS = new BlockVec(1, 2, 3);

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
        var node = LegacyTriggers.checkpoint("cp", POS, data(actions, false));

        assertEquals(actions.size() + 1, node.connections().size());
        assertTrue(node.connections().stream().allMatch(c -> c.output().equals(TriggerNode.ENTER)));
        for (int i = 0; i < actions.size(); i++)
            assertSame(actions.get(i), node.connections().get(i).params());
        var checkpoint = assertInstanceOf(
            CheckpointPlayerInput.class,
            node.connections().getLast().params()
        );
        assertSame(lastRespawn, checkpoint.respawn());
        assertFalse(checkpoint.temporary());

        assertFalse(node.once());
        assertEquals(new LegacyRules(4, true, false, true, lastRespawn), node.legacy());
    }

    @Test
    void teleportingCheckpointDoesNotFaceOnExit() {
        var node = LegacyTriggers.checkpoint(
            "cp",
            POS,
            data(List.of(new TeleportAction(RelativePos.ORIGIN)), false)
        );

        assertFalse(node.legacy().faceOnExit());
        assertNull(
            assertInstanceOf(
                CheckpointPlayerInput.class,
                node.connections().getLast().params()
            ).respawn()
        );
    }

    @Test
    void statusIsOnlyItsActions() {
        var node = LegacyTriggers.status("st", POS, data(List.of(new ChatAction("a")), false));

        assertEquals(
            List.of(ChatAction.KEY),
            node.connections().stream().map(Connection::input).toList()
        );
        assertTrue(node.once());
        assertEquals(new LegacyRules(-1, false, true, false, null), node.legacy());
    }

    @Test
    void repeatableStatusIsNotOnce() {
        assertFalse(LegacyTriggers.status("st", POS, data(List.of(), true)).once());
    }

    @Test
    void finishOnlyFinishes() {
        var finish = LegacyTriggers.finish("f", POS);

        assertInstanceOf(FinishPlayerInput.class, finish.connections().getFirst().params());
        assertEquals(1, finish.connections().size());
        assertEquals(LegacyRules.NONE, finish.legacy());
        assertFalse(finish.once());
    }

    @Test
    void resetOnlyResets() {
        var node = LegacyTriggers.reset("r", POS, true);

        assertTrue(
            assertInstanceOf(ResetPlayerInput.class, node.connections().getFirst().params()).hard()
        );
        assertEquals(1, node.connections().size());
        assertEquals(LegacyRules.NONE, node.legacy());
    }

    private static ActionTriggerData data(List<Action> actions, boolean repeatable) {
        return new ActionTriggerData(
            new ActionList(actions),
            repeatable,
            ActionTriggerCondition.DEFAULT
        );
    }
}
