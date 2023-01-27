package net.hollowcube.terraform.instance;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.batch.Batch;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class SchemChunkBatch implements Block.Setter {
    public static final System.Logger logger = System.getLogger(SchemChunkBatch.class.getName());

    private final Int2ObjectMap<Block> blocks = new Int2ObjectOpenHashMap<>();

    @Override
    public void setBlock(int x, int y, int z, @NotNull Block block) {
        final int index = ChunkUtils.getBlockIndex(x, y, z);
        synchronized (blocks) {
            this.blocks.put(index, block);
        }
    }

    public CompletableFuture<@NotNull Schematic> apply(@NotNull Instance instance, int chunkX, int chunkZ) {
        final Chunk chunk = instance.getChunk(chunkX, chunkZ);
        if (chunk == null || !chunk.isLoaded()) {
            logger.log(System.Logger.Level.WARNING, "Unable to apply ChunkBatch to unloaded chunk ({0}, {1}) in {2}.",
                    chunkX, chunkZ, instance.getUniqueId());
            return CompletableFuture.failedFuture(new IllegalStateException("Chunk is not loaded."));
        }
        return CompletableFuture.supplyAsync(() -> singleThreadFlush(instance, chunk), Batch.BLOCK_BATCH_POOL);
    }

    /**
     * Applies this batch in the current thread, executing the callback upon completion.
     */
    private @NotNull Schematic singleThreadFlush(Instance instance, Chunk chunk) {
        try {
            var inverse = new SchematicBuilder();

            if (blocks.isEmpty()) {
                // Nothing to flush
                return inverse.toSchematic();
            }

            final IntSet sections = new IntArraySet();
            synchronized (blocks) {
                //TODO: LOCKING THE CHUNK HERE IS KINDA BAD
                synchronized (chunk) {
                    for (var entry : blocks.int2ObjectEntrySet()) {
                        final int position = entry.getIntKey();
                        final Block block = entry.getValue();
                        final int section = apply(chunk, position, block, inverse);
                        sections.add(section);
                    }
                }
            }

            updateChunk(instance, chunk, sections);
            return inverse.toSchematic();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private int apply(@NotNull Chunk chunk, int index, Block block, @NotNull SchematicBuilder inverse) {
        final int x = ChunkUtils.blockIndexToChunkPositionX(index);
        final int y = ChunkUtils.blockIndexToChunkPositionY(index);
        final int z = ChunkUtils.blockIndexToChunkPositionZ(index);

        Block prevBlock = chunk.getBlock(x, y, z);
        inverse.addBlock(new Vec(x, y, z), prevBlock);

        chunk.setBlock(x, y, z, block);
        return ChunkUtils.getChunkCoordinate(y);
    }

    private void updateChunk(@NotNull Instance instance, Chunk chunk, IntSet updatedSections) {
        // Refresh chunk for viewers
        // TODO update all sections from `updatedSections`
        chunk.sendChunk();

        if (instance instanceof InstanceContainer) {
            // FIXME: put method in Instance instead
            ((InstanceContainer) instance).refreshLastBlockChangeTime();
        }
    }
}
