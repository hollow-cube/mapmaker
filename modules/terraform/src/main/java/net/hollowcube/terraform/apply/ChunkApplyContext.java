package net.hollowcube.terraform.apply;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.hollowcube.terraform.apply.ApplyEngine.ApplyContext;
import net.hollowcube.terraform.apply.ApplyEngine.SectionRef;
import net.hollowcube.terraform.buffer.BufferSection;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.util.BlockUtil;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.entity.Player;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Section;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import net.minestom.server.instance.palette.Palette;
import net.minestom.server.network.NetworkBuffer;
import net.minestom.server.network.packet.server.SendablePacket;
import net.minestom.server.network.packet.server.ServerPacket;
import net.minestom.server.network.packet.server.play.BlockEntityDataPacket;
import net.minestom.server.network.packet.server.play.ChunkBiomesPacket;
import net.minestom.server.network.packet.server.play.MultiBlockChangePacket;
import net.minestom.server.utils.PacketSendingUtils;
import net.minestom.server.utils.block.BlockUtils;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.IntFunction;

import static net.minestom.server.coordinate.CoordConversion.*;

/// Terrible terrible crimes :-(
///
/// Applies one chunk column of an apply. Should be used once and always under the chunk lock.
@SuppressWarnings("UnstableApiUsage")
final class ChunkApplyContext {
    /// Limit for how many block changes in one chunk changes to a full chunk resend as opposed to a
    /// section-level MultiBlockChange. Its kinda vibes based bc the client doesnt handle MBC that well
    /// and can get very laggy.
    private static final int MBC_FULL_CHUNK_LIMIT = 2048;

    private final ApplyContext context;
    private final Chunk chunk;
    private final Int2ObjectMap<Block> entries;
    private final Int2ObjectMap<Block> tickable;
    private final HeightmapChanges heights;

    // Packets, only collected when the chunk had viewers before the write. `mbc` goes null (and recording
    // stops) once the changes are better sent as one full chunk.
    private final boolean viewed;
    private boolean biomesChanged;
    private int changeCount;
    private @Nullable Int2ObjectMap<LongArrayList> mbc; // sectionY → MultiBlockChange entries
    private @Nullable List<BlockEntityUpdate> blockEntities;

    ChunkApplyContext(
        ApplyContext context,
        Chunk chunk,
        Function<Chunk, Collection<HeightmapView>> heightmaps
    ) {
        this.context = context;
        this.chunk = chunk;
        this.entries = ChunkHack.entries(chunk);
        this.tickable = ChunkHack.tickableMap(chunk);
        this.heights = new HeightmapChanges(chunk, heightmaps);
        this.viewed = context.anyViewers && !chunk.getViewers().isEmpty();
        this.mbc = viewed ? new Int2ObjectOpenHashMap<>() : null;
    }

    /// Applies the sections to the chunk. Returns whether the chunk changed.
    boolean applyChunk(List<SectionRef> sections) {
        boolean dirty;
        chunk.lockWriteLock();
        try {
            if (!chunk.isLoaded()) {
                // Minestom doesn't have a way to guarantee that chunks dont get unloaded
                // currently so we just fail if that happens for any reason.
                throw new IllegalStateException(
                    "apply target chunk unloaded: %d, %d".formatted(
                        chunk.getChunkX(),
                        chunk.getChunkZ()
                    )
                );
            }

            int minSection = chunk.getMinSection(), maxSection = chunk.getMaxSection();
            long before = context.changed;
            try {
                for (SectionRef ref : sections) {
                    if (ref.sy() < minSection || ref.sy() >= maxSection) continue;

                    var worldSection = chunk.getSection(ref.sy());
                    applySectionBlocks(ref, worldSection.blockPalette());
                    applySectionBiomes(ref, worldSection.biomePalette());
                }
            } finally {
                heights.refresh();
                dirty = context.changed != before || biomesChanged;
                if (dirty) {
                    chunk.invalidate();
                    ChunkHack.clearOcclusion(chunk);
                }
            }
        } finally {
            chunk.unlockWriteLock();
        }

        // Instance#getPlayers is a plain set, unlike Chunk#getViewers, so check it before any viewer scan.
        if (dirty && !chunk.getInstance().getPlayers().isEmpty()) {
            // Chunk#getViewers recomputes from player positions on every iteration, so take it once.
            List<Player> viewers = List.copyOf(chunk.getViewers());
            if (viewed) sendPackets(viewers);
            else if (!viewers.isEmpty()) {
                // Technically we race a viewer being added with our tracking, so if we hit
                // that, just send the full chunk and call it a day.
                send(viewers, chunk.getFullDataPacket());
            }
        }
        return dirty;
    }

    private void applySectionBlocks(SectionRef ref, Palette world) {
        final BufferSection section = ref.section();
        if (!section.hasBlocks()) return;

        final int sy = ref.sy();
        final int baseX = ref.sx() << 4, baseY = sy << 4, baseZ = ref.sz() << 4;
        final long before = context.changed;

        // Fastpath for filling an entire section with one block
        final int uniform = section.uniformBlockValue();
        if (uniform > 0 || section.count() == SECTION_BLOCK_COUNT) {
            try {
                // plain state is a block that needs no chunk entries
                boolean isPlainState = uniform > 0
                    && !context.needsEntry(uniform - 1)
                    && !section.hasBlockEntities();
                if (isPlainState) applyUniformFill(ref, world, uniform - 1);
                else applyFullCover(ref, world, baseX, baseY, baseZ);
            } finally {
                if (context.changed != before) heights.sectionChanged(sy);
            }
            if (context.changed != before) {
                // Send the full chunk rather than mbc
                mbc = null;
                blockEntities = null;
            }
            return;
        }

        // Slow path partial replacement :(
        section.forEachBlockState((rx, ry, rz, newStateId) -> {
            final int oldStateId = world.get(rx, ry, rz);
            final int absX = baseX + rx, absY = baseY + ry, absZ = baseZ + rz;
            final int idx = chunkBlockIndex(absX, absY, absZ);
            final var newEntry = section.getBlockEntity(rx, ry, rz);
            final var oldEntry = entries.get(idx);

            // Neither side carries block-entity data and the new state needs no chunk entry: compare state
            // ids directly.
            if (newEntry == null && oldEntry == null && !context.needsEntry(newStateId)) {
                if (oldStateId == newStateId) return;

                heights.blockChanged(rx, absY, rz);
                world.set(rx, ry, rz, newStateId);
                if (context.undo != null) context.undo.setBlockState(absX, absY, absZ, oldStateId);
                context.changed++;
                recordBlockChangeForSend(sy, rx, ry, rz, newStateId);
                return;
            }

            // Full-block compare and unconditional entry reconciliation. The handler is attached before the
            // compare, so a world block already carrying the resolved handler is a clean skip.
            final Block newBlock = context.attach(
                newEntry != null ? newEntry : Block.fromStateId(newStateId)
            );
            final Block oldBlock = oldEntry != null ? oldEntry : Block.fromStateId(oldStateId);
            if (BlockUtil.sameBlock(newBlock, oldBlock)) return;

            if (newStateId != oldStateId) {
                heights.blockChanged(rx, absY, rz);
                world.set(rx, ry, rz, newStateId);
            }
            if (BlockUtil.isPlainState(newBlock)) entries.remove(idx);
            else entries.put(idx, newBlock);

            final BlockHandler handler = newBlock.handler();
            if (handler != null && handler.isTickable()) tickable.put(idx, newBlock);
            else tickable.remove(idx);

            if (context.undo != null) context.undo.setBlockState(absX, absY, absZ, oldBlock);
            context.changed++;
            if (newStateId != oldStateId) recordBlockChangeForSend(sy, rx, ry, rz, newStateId);
            if (newBlock.blockEntityType() != null)
                recordBlockEntityForSend(absX, absY, absZ, newBlock);

            context.fireDestroy(oldBlock, newBlock, absX, absY, absZ);
            context.firePlace(newBlock, oldBlock, absX, absY, absZ);
        });
    }

    /// Fastpath where a full section replace doesn't need block entries (ie plain state)
    private void applyUniformFill(SectionRef ref, Palette world, int newStateId) {
        var sectionEntries = collectSectionEntries(ref.sy());

        long sameCount = world.count(newStateId);
        if (sameCount == SECTION_BLOCK_COUNT && sectionEntries == null)
            return; // Same contents already

        if (sectionEntries != null) {
            // Entries which have the same state but are losing a block entry should not be considered the same.
            for (IntIterator it = sectionEntries.iterator(); it.hasNext();) {
                if (entries.get(it.nextInt()).stateId() == newStateId) sameCount--;
            }
        }

        Block fillBlock = Block.fromStateId(newStateId);

        if (context.undo != null) context.undo.captureSection(ref.sx(), ref.sy(), ref.sz(), world);
        moveBlockEntriesInto(
            context.undo,
            sectionEntries,
            ref.sx() << 4,
            ref.sz() << 4,
            _ -> fillBlock
        );
        world.fill(newStateId);
        context.changed += SECTION_BLOCK_COUNT - sameCount;
    }

    /// Fastpath where a full section replace is non-uniform or has block entries.
    private void applyFullCover(SectionRef ref, Palette world, int baseX, int baseY, int baseZ) {
        var section = ref.section();

        // We need to capture for count & firing handlers even without undo, so make a temp buffer.
        var captureBuffer = context.undo != null ? context.undo : EditBuffer.builder();
        var undoSection = captureBuffer.captureSection(ref.sx(), ref.sy(), ref.sz(), world);
        moveBlockEntriesInto(captureBuffer, collectSectionEntries(ref.sy()), baseX, baseZ, idx -> {
            int rx = chunkBlockIndexGetX(idx), ry = chunkBlockIndexGetY(idx) & 15;
            int rz = chunkBlockIndexGetZ(idx);
            Block entry = section.getBlockEntity(rx, ry, rz);
            return context.attach(
                entry != null ? entry : Block.fromStateId(section.getBlockState(rx, ry, rz))
            );
        });

        section.copyBlocksInto(world);
        context.changed += countFullCoverChanges(undoSection, section);

        // Entries the bulk copy can't carry: the buffer's side-map blocks, and bare states that still need one
        // (registry block entities, host handlers). Only walk every cell when some state needs an entry.
        if (section.containsBlockState(context::needsEntry)) {
            section.forEachBlockState((rx, ry, rz, stateId) -> {
                Block block = section.getBlockEntity(rx, ry, rz);
                if (block == null && !context.needsEntry(stateId)) return;
                placeEntry(
                    block != null ? block : Block.fromStateId(stateId),
                    undoSection,
                    baseX + rx,
                    baseY + ry,
                    baseZ + rz
                );
            });
        } else {
            section.forEachBlockEntity(
                (rx, ry, rz, block) -> placeEntry(
                    block,
                    undoSection,
                    baseX + rx,
                    baseY + ry,
                    baseZ + rz
                )
            );
        }
    }

    /// Writes a full-cover cell's chunk entry, with its handler attached, and fires its onPlace.
    private void placeEntry(Block block, BufferSection undoSection, int x, int y, int z) {
        Block attached = context.attach(block);
        int idx = chunkBlockIndex(x, y, z);
        entries.put(idx, attached);
        BlockHandler handler = attached.handler();
        if (handler != null && handler.isTickable()) tickable.put(idx, attached);
        firePlaceOver(attached, undoSection, x, y, z);
    }

    /// Cells a full-cover write changed, comparing the captured undo section with the written one. A cell
    /// keeping its state counts only when its side-map block differs, so re-applying identical content
    /// counts nothing and leaves the chunk clean.
    private long countFullCoverChanges(BufferSection before, BufferSection after) {
        long count = 0;
        for (int y = 0; y < 16; y++) {
            for (int z = 0; z < 16; z++) {
                for (int x = 0; x < 16; x++) {
                    int oldState = before.getBlockState(x, y, z);
                    int newState = after.getBlockState(x, y, z);
                    if (oldState != newState) {
                        count++;
                        continue;
                    }

                    Block oldEntry = before.getBlockEntity(x, y, z);
                    Block newEntry = after.getBlockEntity(x, y, z);
                    if (oldEntry == null && newEntry == null) continue;
                    Block oldBlock = oldEntry != null ? oldEntry : Block.fromStateId(oldState);
                    Block newBlock = context.attach(
                        newEntry != null ? newEntry : Block.fromStateId(newState)
                    );
                    if (!BlockUtil.sameBlock(newBlock, oldBlock)) count++;
                }
            }
        }
        return count;
    }

    /// Fires the written block's onPlace with the previous block read from the captured undo section. The
    /// bulk paths' place half, [#moveBlockEntriesInto] fires the destroy half.
    private void firePlaceOver(Block attached, BufferSection undoSection, int x, int y, int z) {
        if (attached.handler() == null) return;

        Block previousBe = undoSection.getBlockEntity(x & 15, y & 15, z & 15);
        Block previous = previousBe != null
            ? previousBe
            : Block.fromStateId(undoSection.getBlockState(x & 15, y & 15, z & 15));
        if (!BlockUtil.sameBlock(attached, previous))
            context.firePlace(attached, previous, x, y, z);
    }

    /// Applies the section's biomes into the world biome palette, capturing undo.
    private void applySectionBiomes(SectionRef ref, Palette worldBiomes) {
        if (!ref.section().hasBiomes()) return;

        final int baseX = ref.sx() << 4, baseY = ref.sy() << 4, baseZ = ref.sz() << 4;
        ref.section().forEachBiome((bx, by, bz, biomeId) -> {
            int old = worldBiomes.get(bx, by, bz);
            if (old == biomeId) return;

            worldBiomes.set(bx, by, bz, biomeId);
            if (context.undo != null)
                context.undo.setBiome(baseX + (bx << 2), baseY + (by << 2), baseZ + (bz << 2), old);
            biomesChanged = true;
        });
    }

    /// The keys of block entities whose Y falls in this section, or null if none.
    private @Nullable IntList collectSectionEntries(int sectionY) {
        if (entries.isEmpty()) return null;

        IntList keys = null;
        for (IntIterator it = entries.keySet().iterator(); it.hasNext();) {
            int idx = it.nextInt();
            if (chunkBlockIndexGetY(idx) >> 4 == sectionY) {
                if (keys == null) keys = new IntArrayList();
                keys.add(idx);
            }
        }
        return keys;
    }

    /// Captures the given entries into `capture`'s captured section, when given, and removes them from the
    /// world maps, firing each wiped handler's onDestroy. `replacement` resolves the block overwriting a cell;
    /// a cell rewritten with the same block fires nothing.
    private void moveBlockEntriesInto(
        EditBuffer.@Nullable Builder capture,
        @Nullable IntList keys,
        int baseX,
        int baseZ,
        IntFunction<Block> replacement
    ) {
        if (keys == null) return;

        for (IntIterator it = keys.iterator(); it.hasNext();) {
            int idx = it.nextInt();
            Block old = entries.remove(idx);
            tickable.remove(idx);
            int rx = chunkBlockIndexGetX(idx);
            int y = chunkBlockIndexGetY(idx);
            int rz = chunkBlockIndexGetZ(idx);
            if (capture != null) capture.captureBlockEntity(baseX + rx, y, baseZ + rz, old);
            if (old.handler() == null) continue;
            Block next = replacement.apply(idx);
            if (!BlockUtil.sameBlock(old, next))
                context.fireDestroy(old, next, baseX + rx, y, baseZ + rz);
        }
    }

    private void recordBlockChangeForSend(int sectionY, int rx, int ry, int rz, int stateId) {
        if (mbc == null) return;
        if (++changeCount > MBC_FULL_CHUNK_LIMIT) {
            mbc = null;
            blockEntities = null;
            return;
        }
        mbc.computeIfAbsent(sectionY, _ -> new LongArrayList())
            .add(encodeSectionBlockChange(rx, ry, rz, stateId));
    }

    private void recordBlockEntityForSend(int x, int y, int z, Block block) {
        if (mbc == null) return;
        if (blockEntities == null) blockEntities = new ArrayList<>();
        blockEntities.add(new BlockEntityUpdate(x, y, z, block));
    }

    private void sendPackets(List<Player> viewers) {
        if (mbc == null) {
            send(viewers, chunk.getFullDataPacket());
            return;
        }

        for (var entry : Int2ObjectMaps.fastIterable(mbc)) {
            send(
                viewers,
                new MultiBlockChangePacket(
                    chunk.getChunkX(),
                    entry.getIntKey(),
                    chunk.getChunkZ(),
                    entry.getValue().toLongArray()
                )
            );
        }
        if (blockEntities != null) {
            for (var update : blockEntities) {
                send(
                    viewers,
                    new BlockEntityDataPacket(
                        new Vec(update.x(), update.y(), update.z()),
                        update.block().blockEntityType(),
                        BlockUtils.extractClientNbt(update.block())
                    )
                );
            }
        }
        if (biomesChanged) sendChunkBiomes(viewers);
    }

    /// Sends the whole column's biome palettes as a [ChunkBiomesPacket] instead of a full chunk resend.
    private void sendChunkBiomes(List<Player> viewers) {
        var serializer = Palette.biomeSerializer(chunk.getInstance().registries().biome().size());
        byte[] data;
        chunk.lockReadLock();
        try {
            data = NetworkBuffer.makeArray(buffer -> {
                for (Section section : chunk.getSections())
                    buffer.write(serializer, section.biomePalette());
            });
        } finally {
            chunk.unlockReadLock();
        }

        send(
            viewers,
            new ChunkBiomesPacket(
                List.of(
                    new ChunkBiomesPacket.ChunkBiomeData(chunk.getChunkX(), chunk.getChunkZ(), data)
                )
            )
        );
    }

    private static void send(List<Player> viewers, SendablePacket packet) {
        if (packet instanceof ServerPacket serverPacket)
            PacketSendingUtils.sendGroupedPacket(viewers, serverPacket);
        else for (Player viewer : viewers) viewer.sendPacket(packet);
    }

    private record BlockEntityUpdate(int x, int y, int z, Block block) {}
}
