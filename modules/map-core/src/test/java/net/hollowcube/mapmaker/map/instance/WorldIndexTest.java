package net.hollowcube.mapmaker.map.instance;

import net.hollowcube.mapmaker.map.MapWorld;
import net.hollowcube.mapmaker.map.block.CollidableBlock;
import net.hollowcube.mapmaker.map.block.Trigger;
import net.hollowcube.mapmaker.map.entity.PhysicsTestEnv;
import net.hollowcube.mapmaker.map.util.spatial.BoundingBox;
import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.mapmaker.map.ReadableMapData;
import net.hollowcube.polar.PolarWorldAccess;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.nbt.CompoundBinaryTag;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.block.Block;
import net.minestom.server.world.DimensionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.channels.Channels;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

class WorldIndexTest {
    static final CollidableBlock PLATE = new TestCollidable(Key.key("test:plate"),
        new net.minestom.server.collision.BoundingBox(14.0 / 16.0, 4.0 / 16.0, 14.0 / 16.0));
    static final Block PLATE_BLOCK = Block.STONE_PRESSURE_PLATE.withHandler(PLATE);

    private final java.util.List<MapInstance> instances = new ArrayList<>();

    @BeforeAll
    static void init() {
        PhysicsTestEnv.init();
        MinecraftServer.getBlockManager().registerHandler(PLATE.getKey(), () -> PLATE);
    }

    @AfterEach
    void cleanup() {
        instances.forEach(MapInstance::unload);
    }

    @Test
    void registersCollidableBlocks() {
        var instance = createInstance();
        instance.setBlock(1, 41, 2, PLATE_BLOCK);
        var index = instance.index();
        assertNull(index.block(new BlockVec(1, 41, 2)), "changes apply at update");

        index.update(instance, MapSize.NORMAL);
        var node = index.block(new BlockVec(1, 41, 2));
        assertNotNull(node);
        assertSame(PLATE, node.handler());
        assertEquals(java.util.List.of(node), index.tree().intersectingObjects(new BoundingBox(1, 41, 2, 2, 42, 3)));
    }

    @Test
    void ignoresBlocksWithoutCollidableHandler() {
        var instance = createInstance();
        instance.setBlock(0, 41, 0, Block.STONE_PRESSURE_PLATE.withNbt(CompoundBinaryTag.builder().putString("a", "b").build()));
        instance.setBlock(1, 41, 0, Block.CHEST);
        instance.index().update(instance, MapSize.NORMAL);
        assertTrue(instance.index().blocks().isEmpty());
    }

    @Test
    void removesReplacedBlocks() {
        var instance = createInstance();
        var index = instance.index();
        instance.setBlock(0, 41, 0, PLATE_BLOCK);
        index.update(instance, MapSize.NORMAL);

        instance.setBlock(0, 41, 0, Block.AIR);
        index.update(instance, MapSize.NORMAL);
        assertNull(index.block(new BlockVec(0, 41, 0)));
        assertTrue(index.tree().intersectingObjects(new BoundingBox(0, 41, 0, 1, 42, 1)).isEmpty());
    }

    @Test
    void appliesChangesInOrder() {
        // Terraform sets every changed block to air before writing the new one, and undo restores the old block
        // the same way, so a plate removed and restored before the next update must still be indexed.
        var instance = createInstance();
        var index = instance.index();
        instance.setBlock(0, 41, 0, PLATE_BLOCK);
        index.update(instance, MapSize.NORMAL);

        instance.setBlock(0, 41, 0, Block.AIR);
        instance.setBlock(0, 41, 0, PLATE_BLOCK.withNbt(CompoundBinaryTag.builder().putInt("restored", 1).build()));
        index.update(instance, MapSize.NORMAL);
        assertNotNull(index.block(new BlockVec(0, 41, 0)));
    }

    @Test
    void acceptsChangesFromOtherThreads() throws Exception {
        var instance = createInstance();
        var expected = new HashSet<BlockVec>();
        try (var executor = Executors.newFixedThreadPool(4)) {
            var futures = new ArrayList<CompletableFuture<Void>>();
            for (int thread = 0; thread < 4; thread++) {
                int x = thread * 4;
                for (int z = 0; z < 16; z++) expected.add(new BlockVec(x, 41, z));
                futures.add(CompletableFuture.runAsync(() -> {
                    for (int z = 0; z < 16; z++) instance.setBlock(x, 41, z, PLATE_BLOCK);
                }, executor));
            }
            CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).get();
        }

        instance.index().update(instance, MapSize.NORMAL);
        var indexed = new HashSet<BlockVec>();
        instance.index().blocks().forEach(node -> indexed.add(node.position()));
        assertEquals(expected, indexed);
    }

    @Test
    void registersBlocksLoadedFromPolar() {
        var source = createInstance();
        source.setBlock(3, 41, 5, PLATE_BLOCK.withNbt(CompoundBinaryTag.builder().putString("data", "x").build()));
        var bytes = source.save(PolarWorldAccess.DEFAULT);

        var loaded = track(new MapInstance("test", DimensionType.OVERWORLD, MapInstance.LightingMode.LOADED));
        loaded.load(bytes, null);
        loaded.index().update(loaded, MapSize.NORMAL);
        var node = loaded.index().block(new BlockVec(3, 41, 5));
        assertNotNull(node);
        assertSame(PLATE, node.handler());
        assertEquals(new DataTrigger("x"), node.trigger());
    }

    @Test
    void registersBlocksStreamLoadedFromPolar() {
        var source = createInstance();
        source.setBlock(3, 41, 5, PLATE_BLOCK.withNbt(CompoundBinaryTag.builder().putString("data", "x").build()));
        var bytes = source.save(PolarWorldAccess.DEFAULT);

        var loaded = track(new MapInstance("test", DimensionType.OVERWORLD, MapInstance.LightingMode.LOADED));
        loaded.loadStream(new ReadableMapData(Channels.newChannel(new ByteArrayInputStream(bytes)), bytes.length), null);
        loaded.index().update(loaded, MapSize.NORMAL);
        var block = loaded.index().block(new BlockVec(3, 41, 5));
        assertNotNull(block);
        assertSame(PLATE, block.handler());
        assertEquals(new DataTrigger("x"), block.trigger());
    }

    @Test
    void rebuildsTriggerWhenDataChanges() {
        var instance = createInstance();
        instance.setBlock(1, 41, 2, PLATE_BLOCK.withNbt(CompoundBinaryTag.builder().putString("data", "a").build()));
        instance.index().update(instance, MapSize.NORMAL);
        instance.setBlock(1, 41, 2, PLATE_BLOCK.withNbt(CompoundBinaryTag.builder().putString("data", "b").build()));
        instance.index().update(instance, MapSize.NORMAL);

        var node = instance.index().block(new BlockVec(1, 41, 2));
        assertNotNull(node);
        assertEquals(new DataTrigger("b"), node.trigger());
    }

    @Test
    void skipsBlocksWhoseTriggerFails() {
        var instance = createInstance();
        instance.setBlock(1, 41, 2, PLATE_BLOCK);
        instance.index().update(instance, MapSize.NORMAL);
        instance.setBlock(1, 41, 2, PLATE_BLOCK.withNbt(CompoundBinaryTag.builder().putBoolean("broken", true).build()));
        instance.setBlock(4, 41, 4, PLATE_BLOCK);
        instance.index().update(instance, MapSize.NORMAL);

        assertNull(instance.index().block(new BlockVec(1, 41, 2)));
        assertNotNull(instance.index().block(new BlockVec(4, 41, 4)));
        assertEquals(1, instance.index().tree().intersectingObjects(new BoundingBox(0, 40, 0, 16, 50, 16)).size());
    }

    MapInstance createInstance() {
        var instance = track(new MapInstance("test", DimensionType.OVERWORLD, MapInstance.LightingMode.LOADED));
        instance.loadChunk(0, 0).join();
        return instance;
    }

    private MapInstance track(MapInstance instance) {
        instances.add(instance);
        return instance;
    }

    record TestCollidable(Key key, net.minestom.server.collision.BoundingBox collisionBox) implements CollidableBlock {
        @Override
        public Key getKey() {
            return key;
        }

        @Override
        public Trigger createTrigger(BlockVec position, Block block) {
            var nbt = block.nbt();
            if (nbt != null && nbt.getBoolean("broken")) throw new IllegalStateException("broken plate data");
            return new DataTrigger(nbt == null ? "" : nbt.getString("data"));
        }
    }

    record DataTrigger(String data) implements Trigger {
        @Override
        public void onEnter(MapWorld world, Player player) {
        }
    }
}
