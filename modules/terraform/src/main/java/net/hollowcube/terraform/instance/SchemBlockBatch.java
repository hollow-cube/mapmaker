package net.hollowcube.terraform.instance;


import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.hollowcube.util.schem.Rotation;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.block.Block;
import net.minestom.server.utils.chunk.ChunkUtils;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

@SuppressWarnings("UnstableApiUsage")
public class SchemBlockBatch implements Block.Setter {

    // In the form of <Chunk Index, Batch>
    private final Long2ObjectMap<SchemChunkBatch> chunkBatchesMap = new Long2ObjectOpenHashMap<>();
    private final SchematicBuilder schematicBuilder = new SchematicBuilder();

    @Override
    public void setBlock(int x, int y, int z, @NotNull Block block) {
        final int chunkX = ChunkUtils.getChunkCoordinate(x);
        final int chunkZ = ChunkUtils.getChunkCoordinate(z);
        final long chunkIndex = ChunkUtils.getChunkIndex(chunkX, chunkZ);

        final SchemChunkBatch chunkBatch;
        synchronized (chunkBatchesMap) {
            chunkBatch = chunkBatchesMap.computeIfAbsent(chunkIndex, i -> new SchemChunkBatch());
        }

        final int relativeX = x - (chunkX * Chunk.CHUNK_SIZE_X);
        final int relativeZ = z - (chunkZ * Chunk.CHUNK_SIZE_Z);
        chunkBatch.setBlock(relativeX, y, relativeZ, block);
        schematicBuilder.addBlock(new Vec(x, y, z), block);
    }

    public @NotNull CompletableFuture<@NotNull Schematic> apply(@NotNull Instance instance) {
        System.out.println(chunkBatchesMap);
        var schematic = new SchematicBuilder();
        CompletableFuture<Void>[] cfs = new CompletableFuture[chunkBatchesMap.size()];
        synchronized (chunkBatchesMap) {
            int i = 0;
            for (var entry : Long2ObjectMaps.fastIterable(chunkBatchesMap)) {
                final long chunkIndex = entry.getLongKey();
                final int chunkX = ChunkUtils.getChunkCoordX(chunkIndex);
                final int chunkZ = ChunkUtils.getChunkCoordZ(chunkIndex);
                final SchemChunkBatch batch = entry.getValue();
                cfs[i++] = batch.apply(instance, chunkX, chunkZ).thenAccept(schem -> {
                    schem.apply(Rotation.NONE, (point, block) -> {
                        schematic.addBlock(point.add(chunkX * Chunk.CHUNK_SIZE_X, 0, chunkZ * Chunk.CHUNK_SIZE_Z), block);
                    });
                });
            }
        }

        return CompletableFuture.allOf(cfs)
                .thenApply(v -> {
                    if (instance instanceof InstanceContainer) {
                        // FIXME: put method in Instance instead
                        ((InstanceContainer) instance).refreshLastBlockChangeTime();
                    }
                    return schematic.toSchematic();
                });
    }

    public @NotNull Schematic getSchematic() {
        return schematicBuilder.toSchematic();
    }

}
