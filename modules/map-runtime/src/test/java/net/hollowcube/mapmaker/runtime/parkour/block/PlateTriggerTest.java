package net.hollowcube.mapmaker.runtime.parkour.block;

import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionList;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerCondition;
import net.hollowcube.mapmaker.runtime.parkour.action.ActionTriggerData;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerNode;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerNodeData;
import net.hollowcube.mapmaker.runtime.parkour.node.TriggerTemplate;
import net.hollowcube.mapmaker.test.AbstractMapWorldTest;
import net.hollowcube.mapmaker.test.TestMapWorld;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.instance.block.Block;
import net.minestom.testing.Env;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlateTriggerTest extends AbstractMapWorldTest<TestMapWorld> {
    private static final BlockVec POS = new BlockVec(3, 41, -2);
    private static final TriggerNodeData REPEATABLE_STATUS =
        TriggerTemplate.status().withActionData(
            new ActionTriggerData(new ActionList(), true, ActionTriggerCondition.DEFAULT)
        );

    @Override
    protected @NotNull TestMapWorld createWorld(@NotNull Env env) {
        var world = new TestMapWorld();
        world.instance().loadChunk(0, -1).join();
        return world;
    }

    @Test
    void convertedPlateKeepsItsLegacyId() {
        var trigger = index(
            Block.HEAVY_WEIGHTED_PRESSURE_PLATE.withHandler(PressurePlateBlock.INSTANCE)
                .withTag(TriggerNodeData.TAG, TriggerTemplate.checkpoint())
                .withTag(PressurePlateBlock.LEGACY_ID_TAG, "mapmaker:checkpoint_plate")
        );

        assertEquals("mapmaker:checkpoint_plate/3_41_-2", trigger.id());
        assertEquals(POS, trigger.position());
        assertEquals(TriggerTemplate.checkpoint(), trigger.data());
    }

    @Test
    void newPlateUsesItsOwnId() {
        var trigger = index(
            Block.LIGHT_WEIGHTED_PRESSURE_PLATE.withHandler(PressurePlateBlock.INSTANCE)
                .withTag(TriggerNodeData.TAG, REPEATABLE_STATUS)
        );

        assertEquals("mapmaker:pressure_plate/3_41_-2", trigger.id());
        assertFalse(trigger.data().once());
    }

    @Test
    void plateWithoutDataDoesNothing() {
        var trigger = index(
            Block.HEAVY_WEIGHTED_PRESSURE_PLATE.withHandler(PressurePlateBlock.INSTANCE)
        );

        assertEquals(TriggerNodeData.EMPTY, trigger.data());
    }

    private TriggerNode index(Block block) {
        var instance = world.instance();
        instance.setBlock(POS, block);
        instance.index().update(instance, MapSize.NORMAL);
        var node = instance.index().block(POS);
        assertNotNull(node);
        return assertInstanceOf(TriggerNode.class, node.trigger());
    }
}
