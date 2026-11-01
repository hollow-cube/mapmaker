package net.hollowcube.terraform.apply;

import it.unimi.dsi.fastutil.longs.Long2ObjectRBTreeMap;
import net.hollowcube.terraform.buffer.BufferSection;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.kyori.adventure.key.Key;
import net.minestom.server.coordinate.Vec;
import net.minestom.server.instance.Chunk;
import net.minestom.server.instance.DynamicChunk;
import net.minestom.server.instance.Instance;
import net.minestom.server.instance.block.Block;
import net.minestom.server.instance.block.BlockHandler;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static net.minestom.server.coordinate.CoordConversion.*;

/// Applies an [EditBuffer] to an [Instance], quickly.
///
/// Does terrible hackery that depends on the internals of Palette, DynamicChunk, Heightmap.
///
/// Returns the undo buffer for the change that was applied along with the count and specific
/// which were changed during the operation. It does not currently modify lighting, it remains
/// the callers responsibility to update it.
///
/// It is not idempotent, its possible to fail after some changes have been made in which
/// case a [PartialApplyException] will be thrown containing the undo buffer for the partial
/// change.
///
/// @see #DEFAULT
@SuppressWarnings("UnstableApiUsage")
public final class ApplyEngine {
    private static final Logger LOGGER = LoggerFactory.getLogger(ApplyEngine.class);

    /// The outcome of an apply.
    ///
    /// @param changed the number of cells modified (not including noops)
    public record ApplyResult(EditBuffer undo, long changed, List<Chunk> chunks) {}

    public static final ApplyEngine DEFAULT = builder().build();

    public static Builder builder() {
        return new Builder();
    }

    private final BlockHandlerRegistry handlers;
    private final Function<Chunk, Collection<HeightmapView>> heightmaps;
    private final long tickTargetNanos;

    private ApplyEngine(Builder builder) {
        this.handlers = Objects.requireNonNullElseGet(builder.handlers, BlockHandlerRegistry::new);
        this.heightmaps = builder.heightmaps;
        this.tickTargetNanos = builder.tickTargetNanos;
    }

    /// Applies the whole buffer now, on the calling thread. Waits for any chunk loads and block handler
    /// callbacks fire on the calling thread.
    ///
    /// Not ordered wrt [#submit].
    public ApplyResult apply(Instance instance, EditBuffer buffer) {
        return apply(instance, buffer, UnloadedChunkPolicy.FAIL);
    }

    /// Applies the whole buffer now, on the calling thread. Waits for any chunk loads and block handler
    /// callbacks fire on the calling thread.
    ///
    /// Not ordered wrt [#submit].
    public ApplyResult apply(Instance instance, EditBuffer buffer, UnloadedChunkPolicy policy) {
        return apply(instance, buffer, policy, true);
    }

    /// Applies the whole buffer now, on the calling thread. Waits for any chunk loads and block handler
    /// callbacks fire on the calling thread.
    ///
    /// Not ordered wrt [#submit].
    public ApplyResult apply(
        Instance instance,
        EditBuffer buffer,
        UnloadedChunkPolicy policy,
        boolean captureUndo
    ) {
        var work = chunkWork(instance, buffer, policy);
        checkStorage(work); // Waits for every load (all started above, so they run concurrently)

        var context = newContext(instance, captureUndo);
        for (ChunkWork chunkWork : work) applyColumn(context, chunkWork);
        return context.result();
    }

    /// Queues the buffer to be applied on the instance's tick thread within [Builder#tickTarget].
    ///
    /// Unloaded chunks under [UnloadedChunkPolicy#LOAD] are loaded prior to any application starting.
    ///
    /// The future completes on the instance's tick thread, fails may happen off-thread.
    public CompletableFuture<ApplyResult> submit(Instance instance, EditBuffer buffer) {
        return submit(instance, buffer, UnloadedChunkPolicy.FAIL);
    }

    /// Queues the buffer to be applied on the instance's tick thread within [Builder#tickTarget].
    ///
    /// Unloaded chunks under [UnloadedChunkPolicy#LOAD] are loaded prior to any application starting.
    ///
    /// The future completes on the instance's tick thread, fails may happen off-thread.
    public CompletableFuture<ApplyResult> submit(
        Instance instance,
        EditBuffer buffer,
        UnloadedChunkPolicy policy
    ) {
        return submit(instance, buffer, policy, true);
    }

    /// Queues the buffer to be applied on the instance's tick thread within [Builder#tickTarget].
    ///
    /// Unloaded chunks under [UnloadedChunkPolicy#LOAD] are loaded prior to any application starting.
    ///
    /// The future completes on the instance's tick thread, fails may happen off-thread.
    public CompletableFuture<ApplyResult> submit(
        Instance instance,
        EditBuffer buffer,
        UnloadedChunkPolicy policy,
        boolean captureUndo
    ) {
        ApplyQueue.Operation operation;
        try {
            operation = new ApplyQueue.Operation(this, instance, buffer, policy, captureUndo);
        } catch (RuntimeException e) {
            return CompletableFuture.failedFuture(e);
        }
        if (!ApplyQueue.forInstance(instance, tickTargetNanos).submit(operation)) {
            return CompletableFuture.failedFuture(
                new IllegalStateException("instance closed to applies: " + instance.getUuid())
            );
        }
        return operation.result();
    }

    /// Stops accepting [submitted][#submit] applies to the instance and completes when the already submitted
    /// applications have finished.
    ///
    /// The instance must keep ticking for application to happen.
    public CompletableFuture<Void> close(Instance instance) {
        return ApplyQueue.forInstance(instance, tickTargetNanos).close();
    }

    ApplyContext newContext(Instance instance, boolean captureUndo) {
        return new ApplyContext(handlers, instance, captureUndo);
    }

    void applyColumn(ApplyContext context, ChunkWork work) {
        var chunk = work.chunk();
        try {
            var chunkContext = new ChunkApplyContext(context, chunk, heightmaps);
            boolean dirty = chunkContext.applyChunk(work.sections());
            if (dirty || work.loaded()) context.chunks.add(chunk);
        } catch (RuntimeException e) {
            context.chunks.add(chunk);
            // The column that threw may be partly written but not sent, so send the whole thing as is
            if (context.anyViewers && chunk.isLoaded())
                chunk.sendPacketToViewers(chunk.getFullDataPacket());
            throw new PartialApplyException(context.result(), e);
        }
    }

    /// Group the buffer work into chunks, sorted by z-then-x, and start loading any unloaded chunks depending
    /// on the [UnloadedChunkPolicy]. Does not wait for the loads, see [#checkStorage].
    static List<ChunkWork> chunkWork(
        Instance instance,
        EditBuffer buffer,
        UnloadedChunkPolicy policy
    ) {
        var byChunk = new Long2ObjectRBTreeMap<List<SectionRef>>(
            (a, b) -> chunkIndexGetZ(a) != chunkIndexGetZ(b)
                ? Integer.compare(chunkIndexGetZ(a), chunkIndexGetZ(b))
                : Integer.compare(chunkIndexGetX(a), chunkIndexGetX(b))
        );
        buffer.forEachSection(
            (sx, sy, sz, section) -> byChunk
                .computeIfAbsent(chunkIndex(sx, sz), _ -> new ArrayList<>())
                .add(new SectionRef(sx, sy, sz, section))
        );

        var work = new ArrayList<ChunkWork>(byChunk.size());
        for (var entry : byChunk.long2ObjectEntrySet()) {
            int chunkX = chunkIndexGetX(entry.getLongKey());
            int chunkZ = chunkIndexGetZ(entry.getLongKey());
            Chunk chunk = instance.getChunk(chunkX, chunkZ);
            if (chunk != null) {
                work.add(
                    new ChunkWork(CompletableFuture.completedFuture(chunk), false, entry.getValue())
                );
            } else if (policy == UnloadedChunkPolicy.FAIL) {
                throw new IllegalStateException(
                    "apply target chunk not loaded: %d, %d".formatted(chunkX, chunkZ)
                );
            } else {
                work.add(new ChunkWork(instance.loadChunk(chunkX, chunkZ), true, entry.getValue()));
            }
        }
        return work;
    }

    static void checkStorage(List<ChunkWork> work) {
        for (ChunkWork chunkWork : work) {
            Chunk chunk = chunkWork.chunk();
            if (!(chunk instanceof DynamicChunk)) {
                throw new IllegalStateException(
                    "apply target chunk has no block storage (%s): %d, %d".formatted(
                        chunk.getClass().getSimpleName(),
                        chunk.getChunkX(),
                        chunk.getChunkZ()
                    )
                );
            }
        }
    }

    /// Per-apply state
    static final class ApplyContext {
        final EditBuffer.@Nullable Builder undo;
        final List<Chunk> chunks = new ArrayList<>();
        final boolean anyViewers;
        long changed;
        private final BlockHandlerRegistry registry;
        private final Instance instance;
        private boolean warned;

        ApplyContext(BlockHandlerRegistry registry, Instance instance, boolean captureUndo) {
            this.registry = registry;
            this.instance = instance;
            this.undo = captureUndo ? EditBuffer.builder() : null;
            this.anyViewers = !instance.getPlayers().isEmpty();
        }

        ApplyResult result() {
            return new ApplyResult(
                undo != null ? undo.build() : EditBuffer.empty(),
                changed,
                List.copyOf(chunks)
            );
        }

        Block attach(Block block) {
            return registry.attach(block);
        }

        boolean needsEntry(int stateId) {
            return registry.needsEntry(stateId);
        }

        void fireDestroy(Block old, Block replacement, int x, int y, int z) {
            BlockHandler handler = old.handler();
            if (handler == null) return;
            try {
                handler.onDestroy(
                    new BlockHandler.Destroy(old, replacement, instance, new Vec(x, y, z))
                );
            } catch (Throwable t) {
                warnOnce("onDestroy", old, x, y, z, t);
            }
        }

        void firePlace(Block block, Block previous, int x, int y, int z) {
            BlockHandler handler = block.handler();
            if (handler == null) return;
            try {
                handler.onPlace(
                    new BlockHandler.Placement(block, previous, instance, new Vec(x, y, z))
                );
            } catch (Throwable t) {
                warnOnce("onPlace", block, x, y, z, t);
            }
        }

        private void warnOnce(String callback, Block block, int x, int y, int z, Throwable t) {
            if (warned) return;
            warned = true;
            LOGGER.warn(
                "block handler {} threw for {} at {},{},{} (further handler throws this apply are not logged)",
                callback,
                block.key(),
                x,
                y,
                z,
                t
            );
        }
    }

    /// One target chunk's sections. `loaded` when the apply loaded the chunk itself.
    record ChunkWork(CompletableFuture<Chunk> future, boolean loaded, List<SectionRef> sections) {
        Chunk chunk() {
            return future.join();
        }
    }

    record SectionRef(int sx, int sy, int sz, BufferSection section) {}

    public static final class Builder {
        private @Nullable BlockHandlerRegistry handlers = null;
        private Function<Chunk, Collection<HeightmapView>> heightmaps = HeightmapView.MINESTOM;
        private long tickTargetNanos = TimeUnit.MILLISECONDS.toNanos(45);

        /// Handlers to attach, by block key, to written blocks that carry none.
        public Builder blockHandlers(Map<Key, ? extends BlockHandler> handlers) {
            this.handlers = new BlockHandlerRegistry(handlers);
            return this;
        }

        /// Supplies the heightmaps maintained for each written chunk.
        @ApiStatus.Internal
        public Builder heightmap(Function<Chunk, Collection<HeightmapView>> heightmaps) {
            this.heightmaps = heightmaps;
            return this;
        }

        /// How long a server tick may run with [#submit]ted applies writing in it, counting what the rest of the
        /// last tick took after them. Each instance still writes at least one chunk column per tick. The first
        /// engine to submit to an instance sets it there.
        public Builder tickTarget(Duration target) {
            this.tickTargetNanos = target.toNanos();
            return this;
        }

        public ApplyEngine build() {
            return new ApplyEngine(this);
        }

        private Builder() {}
    }
}
