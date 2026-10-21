package net.hollowcube.mapmaker.map.instance;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.hollowcube.ipc.map.MapSize;
import net.hollowcube.mapmaker.ExceptionReporter;
import net.hollowcube.mapmaker.map.block.CollidableBlock;
import net.hollowcube.mapmaker.map.block.Trigger;
import net.hollowcube.mapmaker.map.util.spatial.BoundingBox;
import net.hollowcube.mapmaker.map.util.spatial.Octree;
import net.hollowcube.mapmaker.map.util.spatial.SpatialObject;
import net.minestom.server.coordinate.BlockVec;
import net.minestom.server.coordinate.CoordConversion;
import net.minestom.server.coordinate.Point;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import static net.hollowcube.mapmaker.map.util.spatial.Octree.simpleOctree;

public final class WorldIndex {
    static void blockChanged(Instance instance, int x, int y, int z, @Nullable Block previous, Block block) {
        var handler = block.handler() instanceof CollidableBlock collidable ? collidable : null;
        if (handler == null && (previous == null || !(previous.handler() instanceof CollidableBlock))) return;
        if (instance instanceof MapInstance mapInstance)
            mapInstance.index().pendingBlocks.add(new BlockChange(x, y, z, handler, block));
    }

    static void chunkLoaded(Chunk chunk, Int2ObjectMap<Block> entries) {
        if (!(chunk.getInstance() instanceof MapInstance mapInstance)) return;
        Int2ObjectMaps.fastForEach(entries, entry -> {
            var block = entry.getValue();
            if (!(block.handler() instanceof CollidableBlock handler)) return;
            int index = entry.getIntKey();
            mapInstance.index().pendingBlocks.add(new BlockChange(
                chunk.getChunkX() * Chunk.CHUNK_SIZE_X + CoordConversion.chunkBlockIndexGetX(index),
                CoordConversion.chunkBlockIndexGetY(index),
                chunk.getChunkZ() * Chunk.CHUNK_SIZE_Z + CoordConversion.chunkBlockIndexGetZ(index),
                handler, block));
        });
    }

    private final Queue<BlockChange> pendingBlocks = new ConcurrentLinkedQueue<>();
    private final Map<BlockVec, IndexedBlock> blocks = new HashMap<>();
    private final Collection<IndexedBlock> blocksView = Collections.unmodifiableCollection(blocks.values());
    private volatile boolean treeDirty = true;
    private Octree tree = Octree.emptyOctree();

    public void queueTreeRebuild() {
        this.treeDirty = true;
    }

    public Octree tree() {
        return tree;
    }

    public @Nullable IndexedBlock block(Point position) {
        return blocks.get(new BlockVec(position.blockX(), position.blockY(), position.blockZ()));
    }

    public @UnmodifiableView Collection<IndexedBlock> blocks() {
        return blocksView;
    }

    public void update(Instance instance, MapSize mapSize) {
        int scalePowerOfTwo = (int) Math.ceil(Math.log(Math.min(mapSize.size(), 4096)) / Math.log(2));

        BlockChange change;
        while ((change = pendingBlocks.poll()) != null) {
            var position = new BlockVec(change.x, change.y, change.z);
            if (change.handler == null) {
                if (blocks.remove(position) != null) treeDirty = true;
                continue;
            }

            Trigger trigger;
            try {
                trigger = change.handler.createTrigger(position, change.block);
            } catch (Exception e) {
                ExceptionReporter.reportException(e);
                if (blocks.remove(position) != null) treeDirty = true;
                continue;
            }

            var node = new IndexedBlock(position, change.handler, change.block, trigger);
            if (!node.equals(blocks.put(position, node))) treeDirty = true;
        }

        if (!treeDirty) return;
        treeDirty = false;

        List<SpatialObject> objects = new ArrayList<>(blocks.values());
        for (var entity : instance.getEntities().stream().sorted(java.util.Comparator.<net.minestom.server.entity.Entity>comparingDouble(e -> e.getPosition().x()).thenComparingDouble(e -> e.getPosition().y()).thenComparingDouble(e -> e.getPosition().z()).thenComparing(net.minestom.server.entity.Entity::getUuid)).toList()) {
            if (entity.isRemoved() || !(entity instanceof SpatialObject spatial)) continue;
            if (spatial.boundingBox().size().isZero()) continue;
            objects.add(spatial);
        }
        this.tree = simpleOctree(scalePowerOfTwo, objects);
    }

    private record BlockChange(int x, int y, int z, @Nullable CollidableBlock handler, Block block) {
    }

    public record IndexedBlock(BlockVec position, CollidableBlock handler, Block block, Trigger trigger) implements SpatialObject {

        @Override
        public BoundingBox boundingBox() {
            var box = handler.collisionBox();
            double x = position.x() + 0.5, y = position.y(), z = position.z() + 0.5;
            return new BoundingBox(
                (float) (x + box.minX()), (float) (y + box.minY()), (float) (z + box.minZ()),
                (float) (x + box.maxX()), (float) (y + box.maxY()), (float) (z + box.maxZ())
            );
        }

        public boolean touches(Point position, net.minestom.server.collision.BoundingBox box) {
            return box.intersectBox(position.sub(this.position.x() + 0.5, this.position.y(), this.position.z() + 0.5),
                handler.collisionBox());
        }
    }
}
