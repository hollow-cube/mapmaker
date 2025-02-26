package net.hollowcube.compat.axiom.data.buffers;

import com.mojang.datafixers.util.Either;
import it.unimi.dsi.fastutil.ints.Int2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectArrayMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectArrayMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectMap;
import net.hollowcube.compat.axiom.AxiomAPI;
import net.minestom.server.instance.block.Block;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record AxiomBlockBuffer(
    @NotNull Long2ObjectMap<Either<Block, Block[]>> updates,
    @NotNull Long2ObjectMap<Short2ObjectMap<AxiomBlockEntityData>> blockEntities
) implements AxiomBuffer {

    private static final long EOD = 0b1000000000000000000000000010000000000000000000000000100000000000L;
    public static final int MAX_BITS_PER_ENTRY;
    static {
        int bpe = 0;
        for (short id = 24134; id < Short.MAX_VALUE; id++) {
            if (Block.fromStateId(id) == null) {
                bpe = (int) Math.ceil(Math.log(id) / Math.log(2));
                break;
            }
        }

        Check.stateCondition(bpe == 0, "Could not find max bits per entry");
        MAX_BITS_PER_ENTRY = bpe;
    }

    private void addBlocks(long index, @Nullable Block block) {
        this.updates.put(index, Either.left(block));
    }

    private void addBlocks(long index, Block[] blocks) {
        this.updates.put(index, Either.right(blocks));
    }

    private void addBlockEntityData(long index, int size, short offset, AxiomBlockEntityData data) {
        this.blockEntities.computeIfAbsent(index, i -> new Short2ObjectArrayMap<>(size)).put(offset, data);
    }

    public static AxiomBlockBuffer read(NetworkBuffer buffer) {
        var blockBuffer = new AxiomBlockBuffer(new Long2ObjectArrayMap<>(10), new Long2ObjectArrayMap<>(10));

        while (true) {
            long index = buffer.read(NetworkBuffer.LONG);
            if (index == EOD) break;

            byte type = buffer.read(NetworkBuffer.BYTE);

            if (type == 0) {
                int id = buffer.read(NetworkBuffer.VAR_INT);
                Check.stateCondition(buffer.read(NetworkBuffer.LONG_ARRAY).length != 0, "Expected empty data array");

                blockBuffer.addBlocks(
                        index,
                        id == AxiomAPI.EMPTY_BLOCK_STATE ? null : Block.fromStateId(id)
                );
            } else if (type > 0 && type < 9) {
                int bits = Math.max(4, type);
                int[] ids = buffer.read(NetworkBuffer.VAR_INT_ARRAY);
                long[] data = buffer.read(NetworkBuffer.LONG_ARRAY);

                blockBuffer.addBlocks(
                        index,
                        read(data, bits, id -> {
                            int block = ids[id];
                            return block == AxiomAPI.EMPTY_BLOCK_STATE ? null : Block.fromStateId(block);
                        })
                );
            } else {
                long[] data = buffer.read(NetworkBuffer.LONG_ARRAY);
                blockBuffer.addBlocks(
                        index,
                        read(data, MAX_BITS_PER_ENTRY, id ->
                                id == AxiomAPI.EMPTY_BLOCK_STATE ? null : Block.fromStateId(id)
                        )
                );
            }

            var blockEntities = Math.min(4096, buffer.read(NetworkBuffer.VAR_INT));
            for (int j = 0; j < blockEntities; j++) {
                short offset = buffer.read(NetworkBuffer.SHORT);
                int size = buffer.read(NetworkBuffer.VAR_INT);
                byte dict = buffer.read(NetworkBuffer.BYTE);
                byte[] data = buffer.read(NetworkBuffer.BYTE_ARRAY);

                blockBuffer.addBlockEntityData(index, blockEntities, offset, new AxiomBlockEntityData(size, dict, data));
            }
        }

        return blockBuffer;
    }

    private static Block[] read(long[] data, int bits, Int2ObjectFunction<@Nullable Block> getter) {
        int dataSectionLength = (int) Math.ceil(Math.floor(64d / bits));
        long mask = (1L << bits) - 1L;

        Block[] blocks = new Block[4096];

        for (int i = 0; i < blocks.length; i++) {
            int index = i / dataSectionLength;
            int subIndex = i % dataSectionLength;

            blocks[i] = getter.apply((int) ((data[index] >>> (bits * subIndex)) & mask));
        }

        return blocks;
    }
}