package net.hollowcube.mapmaker.runtime.replay.playback;

import dev.hollowcube.replay.event.SetBlockEvent;
import dev.hollowcube.replay.event.SpawnEntityEvent;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.EntityType;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.packet.server.play.BlockChangePacket;
import net.minestom.server.network.packet.server.play.DestroyEntitiesPacket;
import net.minestom.server.network.packet.server.play.SpawnEntityPacket;
import net.minestom.testing.Env;
import net.minestom.testing.EnvTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/// Covers who a scene is visible to, which is the whole of what makes it safe to play two replays
/// in one world.
@EnvTest
class ReplaySceneTest {
    private static final BlockVec BLOCK_POSITION = new BlockVec(1, 41, 1);
    private static final Pos ORIGIN = new Pos(0, 41, 0);

    @Test
    void blocksReachViewersOnlyAndNeverTheInstance(Env env) {
        var instance = env.createFlatInstance();
        var viewerConnection = env.createConnection();
        var viewer = viewerConnection.connect(instance, ORIGIN);
        var bystanderConnection = env.createConnection();
        bystanderConnection.connect(instance, ORIGIN);

        var scene = new ReplayScene(instance, ORIGIN);
        scene.addViewer(viewer);
        var viewerBlocks = viewerConnection.trackIncoming(BlockChangePacket.class);
        var bystanderBlocks = bystanderConnection.trackIncoming(BlockChangePacket.class);

        scene.accept(new SetBlockEvent(BLOCK_POSITION, Block.DIAMOND_BLOCK));

        viewerBlocks.assertSingle(packet -> assertEquals(Block.DIAMOND_BLOCK.stateId(), packet.blockStateId()));
        bystanderBlocks.assertEmpty();
        assertEquals(Block.AIR, instance.getBlock(BLOCK_POSITION));
    }

    @Test
    void addViewerSyncsTheStateTheSceneIsAlreadyIn(Env env) {
        var instance = env.createFlatInstance();
        var connection = env.createConnection();
        var viewer = connection.connect(instance, ORIGIN);

        var scene = new ReplayScene(instance, ORIGIN);
        scene.accept(new SetBlockEvent(BLOCK_POSITION, Block.DIAMOND_BLOCK));
        scene.accept(new SpawnEntityEvent(1, EntityType.PIG, ORIGIN));

        var blocks = connection.trackIncoming(BlockChangePacket.class);
        var spawns = connection.trackIncoming(SpawnEntityPacket.class);
        assertTrue(scene.addViewer(viewer));
        assertFalse(scene.addViewer(viewer));

        blocks.assertSingle(packet -> {
            assertEquals(BLOCK_POSITION, packet.blockPosition());
            assertEquals(Block.DIAMOND_BLOCK.stateId(), packet.blockStateId());
        });
        // The subject, spawned by the scene itself, plus the one the replay asked for.
        spawns.assertCount(2);
        assertEquals(1, scene.getViewers().size());
    }

    @Test
    void removeViewerPutsTheRealWorldBack(Env env) {
        var instance = env.createFlatInstance();
        var connection = env.createConnection();
        var viewer = connection.connect(instance, ORIGIN);
        instance.setBlock(BLOCK_POSITION, Block.STONE);

        var scene = new ReplayScene(instance, ORIGIN);
        scene.addViewer(viewer);
        scene.accept(new SetBlockEvent(BLOCK_POSITION, Block.DIAMOND_BLOCK));

        var blocks = connection.trackIncoming(BlockChangePacket.class);
        var destroys = connection.trackIncoming(DestroyEntitiesPacket.class);
        assertTrue(scene.removeViewer(viewer));
        assertFalse(scene.removeViewer(viewer));

        blocks.assertSingle(packet -> assertEquals(Block.STONE.stateId(), packet.blockStateId()));
        destroys.assertSingle();
        assertTrue(scene.getViewers().isEmpty());
    }

    @Test
    void restoreBlocksKeepsTheEntitiesAndDropsTheBlocks(Env env) {
        var instance = env.createFlatInstance();
        var connection = env.createConnection();
        var viewer = connection.connect(instance, ORIGIN);
        instance.setBlock(BLOCK_POSITION, Block.STONE);

        var scene = new ReplayScene(instance, ORIGIN);
        scene.addViewer(viewer);
        scene.accept(new SetBlockEvent(BLOCK_POSITION, Block.DIAMOND_BLOCK));

        var blocks = connection.trackIncoming(BlockChangePacket.class);
        scene.restoreBlocks();
        blocks.assertSingle(packet -> assertEquals(Block.STONE.stateId(), packet.blockStateId()));
        assertEquals(1, scene.getViewers().size());

        // Nothing is remembered any more, so a new viewer sees the world as it is.
        var lateConnection = env.createConnection();
        var late = lateConnection.connect(instance, ORIGIN);
        var lateBlocks = lateConnection.trackIncoming(BlockChangePacket.class);
        scene.addViewer(late);
        lateBlocks.assertEmpty();
    }

    @Test
    void closeUndoesEverythingForEveryViewer(Env env) {
        var instance = env.createFlatInstance();
        var firstConnection = env.createConnection();
        var first = firstConnection.connect(instance, ORIGIN);
        var secondConnection = env.createConnection();
        var second = secondConnection.connect(instance, ORIGIN);
        instance.setBlock(BLOCK_POSITION, Block.STONE);

        var scene = new ReplayScene(instance, ORIGIN);
        scene.addViewer(first);
        scene.addViewer(second);
        scene.accept(new SetBlockEvent(BLOCK_POSITION, Block.DIAMOND_BLOCK));

        var firstBlocks = firstConnection.trackIncoming(BlockChangePacket.class);
        var secondDestroys = secondConnection.trackIncoming(DestroyEntitiesPacket.class);
        scene.close();

        firstBlocks.assertSingle(packet -> assertEquals(Block.STONE.stateId(), packet.blockStateId()));
        secondDestroys.assertSingle();
        assertTrue(scene.getViewers().isEmpty());
    }

    @Test
    void aViewerInAnotherInstanceIsNotSentBlocks(Env env) {
        var instance = env.createFlatInstance();
        var other = env.createFlatInstance();
        var connection = env.createConnection();
        var viewer = connection.connect(instance, ORIGIN);

        var scene = new ReplayScene(instance, ORIGIN);
        scene.addViewer(viewer);
        viewer.setInstance(other, ORIGIN).join();

        var blocks = connection.trackIncoming(BlockChangePacket.class);
        scene.accept(new SetBlockEvent(BLOCK_POSITION, Block.DIAMOND_BLOCK));
        blocks.assertEmpty();
    }
}
