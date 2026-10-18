package net.hollowcube.mapmaker.runtime.parkour.block;

import net.hollowcube.mapmaker.runtime.parkour.action.ActionTrigger;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerCondition;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.test.AbstractMapWorldTest;
import net.hollowcube.mapmaker.test.TestMapWorld;
import net.hollowcube.ipc.map.MapSize;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlateTriggerTest extends AbstractMapWorldTest<TestMapWorld> {
    private static final BlockVec POS = new BlockVec(3, 41, -2);
    private static final ActionTriggerData DATA = new ActionTriggerData(new ActionList(), true, ActionTriggerCondition.DEFAULT);

    @Override
    protected @NotNull TestMapWorld createWorld(@NotNull Env env) {
        var world = new TestMapWorld();
        world.instance().loadChunk(0, -1).join();
        return world;
    }

    @Test
    void checkpointPlateDecodesOnce() {
        var trigger = index(Block.HEAVY_WEIGHTED_PRESSURE_PLATE.withHandler(CheckpointPlateBlock.INSTANCE)
                .withTag(CheckpointPlateBlock.DATA_TAG, DATA));

        assertEquals(ActionTrigger.Kind.CHECKPOINT, trigger.kind());
        assertEquals("mapmaker:checkpoint_plate/3_41_-2", trigger.id());
        assertEquals(POS, trigger.position());
        assertTrue(trigger.data().repeatable());
    }

    @Test
    void statusPlateDecodesOnce() {
        var trigger = index(Block.LIGHT_WEIGHTED_PRESSURE_PLATE.withHandler(StatusPlateBlock.INSTANCE)
                .withTag(StatusPlateBlock.DATA_TAG, DATA));

        assertEquals(ActionTrigger.Kind.STATUS, trigger.kind());
        assertEquals("mapmaker:status_plate/3_41_-2", trigger.id());
        assertTrue(trigger.data().repeatable());
    }

    @Test
    void plateWithoutDataGetsEmptyTrigger() {
        var trigger = index(Block.HEAVY_WEIGHTED_PRESSURE_PLATE.withHandler(CheckpointPlateBlock.INSTANCE));

        assertFalse(trigger.data().repeatable());
        assertTrue(trigger.data().actions().actions().isEmpty());
    }

    private ActionTrigger index(Block block) {
        var instance = world.instance();
        instance.setBlock(POS, block);
        instance.index().update(instance, MapSize.NORMAL);
        var node = instance.index().block(POS);
        assertNotNull(node);
        return assertInstanceOf(ActionTrigger.class, node.trigger());
    }
}
