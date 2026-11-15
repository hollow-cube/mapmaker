package net.hollowcube.terraform.apply;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.longs.Long2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.hollowcube.terraform.entity.EntityAccessor;
import net.hollowcube.terraform.entity.EntityEdit;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReentrantLock;

import static net.hollowcube.terraform.util.BlockUtil.*;
import static net.minestom.server.coordinate.CoordConversion.*;

/// Tracks changes to an instance as an output buffer to create the current state.
///
/// Must be [#attach]ed before any changes. [#drain()] gives an EditBuffer which will create the current state
/// of the world. It is generally an over approximation (for speed), so may contain some unchanged blocks.
///
/// Mark apis remain public for a host to mark changes invisible to Terraform.
///
/// Thread-safe.
public final class ChangeTracker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeTracker.class);
    private static final Tag<ChangeTracker> TAG = Tag.Transient("terraform:change_tracker");

    private static final int SPARSE_CELL_LIMIT = 64; // above this a section is captured whole

    public static ChangeTracker attach(Instance instance, @Nullable EntityAccessor entities) {
        var tracker = new ChangeTracker(instance, entities);
        var existing = instance.getAndUpdateTag(
            TAG,
            current -> current != null ? current : tracker
        );
        if (existing != null) throw new IllegalStateException("instance is already tracked");
        return tracker;
    }

    public static @Nullable ChangeTracker of(Instance instance) {
        return instance.getTag(TAG);
    }

    private final Instance instance;
    private final @Nullable EntityAccessor entities;

    private final ReentrantLock lock = new ReentrantLock();
    // Section key -> dirty cells, in the order sections were first marked
    private final Long2ObjectLinkedOpenHashMap<DirtySection> sections =
        new Long2ObjectLinkedOpenHashMap<>();
    private Map<UUID, Pos> entityMarks = new LinkedHashMap<>();
    private boolean warnedNoAccessor;

    private ChangeTracker(Instance instance, @Nullable EntityAccessor entities) {
        this.instance = instance;
        this.entities = entities;
    }

    public void markBlock(int x, int y, int z) {
        lock.lock();
        try {
            section(x >> 4, y >> 4, z >> 4).markBlock(y & 15, z & 15, x & 15);
        } finally {
            lock.unlock();
        }
    }

    public void markBiome(int x, int y, int z) {
        lock.lock();
        try {
            section(x >> 4, y >> 4, z >> 4).markBiome((y & 15) >> 2, (z & 15) >> 2, (x & 15) >> 2);
        } finally {
            lock.unlock();
        }
    }

    /// Pos should contain the current position for spawn/edit, or where it was when removed.
    /// Latest wins
    public void markEntity(UUID id, Pos pos) {
        lock.lock();
        try {
            entityMarks.put(id, pos);
        } finally {
            lock.unlock();
        }
    }

    public boolean isEmpty() {
        lock.lock();
        try {
            return sections.isEmpty() && entityMarks.isEmpty();
        } finally {
            lock.unlock();
        }
    }

    public EditBuffer drain() {
        return drain(Integer.MAX_VALUE);
    }

    public EditBuffer drain(int maxWholeSections) {
        Long2ObjectMap<DirtySection> drained = new Long2ObjectOpenHashMap<>();
        Map<UUID, Pos> drainedEntities;
        lock.lock();
        try {
            if (sections.isEmpty() && entityMarks.isEmpty()) return EditBuffer.empty();

            int whole = 0;
            var it = sections.long2ObjectEntrySet().fastIterator();
            while (it.hasNext()) {
                var entry = it.next();
                DirtySection section = entry.getValue();
                if (section.isWhole()) {
                    if (whole >= maxWholeSections) continue;
                    whole++;
                }
                drained.put(entry.getLongKey(), section);
                it.remove();
            }
            drainedEntities = entityMarks;
            entityMarks = new LinkedHashMap<>();
        } finally {
            lock.unlock();
        }

        var out = EditBuffer.builder();
        captureSections(drained, out);
        captureEntities(drainedEntities, out);
        return out.build();
    }

    public void detach() {
        instance.updateTag(TAG, current -> current == this ? null : current);
    }

    void markSections(int chunkX, int chunkZ, List<SectionChange> changes) {
        lock.lock();
        try {
            for (SectionChange change : changes)
                section(chunkX, change.sectionY(), chunkZ).merge(change);
        } finally {
            lock.unlock();
        }
    }

    void markEntities(List<EntityEdit> edits) {
        lock.lock();
        try {
            for (EntityEdit edit : edits) entityMarks.put(edit.id(), edit.pos());
        } finally {
            lock.unlock();
        }
    }

    private DirtySection section(int sectionX, int sectionY, int sectionZ) {
        long key = packBlockPos(sectionX, sectionY, sectionZ);
        return sections.computeIfAbsent(key, _ -> new DirtySection());
    }

    private void captureSections(Long2ObjectMap<DirtySection> drained, EditBuffer.Builder out) {
        // Grouped by chunk so each chunk is read under one lock per kind of capture
        Long2ObjectMap<ChunkCapture> byChunk = new Long2ObjectOpenHashMap<>();
        for (var entry : Long2ObjectMaps.fastIterable(drained)) {
            long key = entry.getLongKey();
            int sx = blockPosGetX(key), sy = blockPosGetY(key), sz = blockPosGetZ(key);
            var capture = byChunk.computeIfAbsent(chunkIndex(sx, sz), _ -> new ChunkCapture());
            DirtySection section = entry.getValue();
            BitSet blocks = section.blocks, biomes = section.biomes;
            if (blocks == null || biomes == null) capture.whole().add(sy);
            else capture.sparse().add(new SectionChange(sy, blocks, biomes));
        }

        int dropped = 0;
        for (var entry : Long2ObjectMaps.fastIterable(byChunk)) {
            long key = entry.getLongKey();
            ChunkCapture capture = entry.getValue();
            Chunk chunk = instance.getChunk(chunkIndexGetX(key), chunkIndexGetZ(key));
            if (chunk == null) {
                dropped += capture.whole().size() + capture.sparse().size();
                continue;
            }
            if (!capture.whole().isEmpty())
                CaptureEngine.captureSections(chunk, capture.whole().toIntArray(), out);
            if (!capture.sparse().isEmpty())
                CaptureEngine.captureCells(chunk, capture.sparse(), out);
        }
        if (dropped > 0) {
            LOGGER.warn(
                "dropped {} changed sections of {} whose chunks unloaded before they were drained",
                dropped,
                instance.getUuid()
            );
        }
    }

    private void captureEntities(Map<UUID, Pos> drained, EditBuffer.Builder out) {
        if (drained.isEmpty()) return;
        EntityAccessor accessor = entities;
        if (accessor == null) {
            if (!warnedNoAccessor) {
                warnedNoAccessor = true;
                LOGGER.warn(
                    "entity changes in {} dropped: no EntityAccessor attached",
                    instance.getUuid()
                );
            }
            return;
        }
        for (var entry : drained.entrySet()) {
            EntityEdit current = accessor.capture(instance, entry.getKey());
            out.addEntityEdit(
                current != null ? current : EntityEdit.remove(entry.getKey(), entry.getValue())
            );
        }
    }

    private record ChunkCapture(IntList whole, List<SectionChange> sparse) {
        ChunkCapture() {
            this(new IntArrayList(), new ArrayList<>());
        }
    }

    private static final class DirtySection {
        // Both null once there are too many block cells to list and the section is captured whole
        @Nullable BitSet blocks = new BitSet(4096);
        @Nullable BitSet biomes = new BitSet(64);
        int blockCount;

        boolean isWhole() {
            return blocks == null;
        }

        void markBlock(int y, int z, int x) {
            BitSet cells = blocks;
            if (cells == null) return;
            int index = y << 8 | z << 4 | x;
            if (cells.get(index)) return;
            cells.set(index);
            if (++blockCount > SPARSE_CELL_LIMIT) promote();
        }

        void markBiome(int y, int z, int x) {
            BitSet cells = biomes;
            if (cells != null) cells.set(y << 4 | z << 2 | x);
        }

        void merge(SectionChange change) {
            BitSet blockCells = blocks, biomeCells = biomes;
            if (blockCells == null || biomeCells == null) return;
            blockCells.or(change.blocks());
            biomeCells.or(change.biomes());
            blockCount = blockCells.cardinality();
            if (blockCount > SPARSE_CELL_LIMIT) promote();
        }

        private void promote() {
            blocks = null;
            biomes = null;
        }
    }
}
