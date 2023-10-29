package net.hollowcube.terraform;

import it.unimi.dsi.fastutil.longs.LongArrayList;
import net.hollowcube.terraform.action.edit.WorldView;
import net.hollowcube.terraform.buffer.BlockBuffer;
import net.hollowcube.terraform.buffer.Palette;
import net.hollowcube.terraform.session.history.Change;
import net.hollowcube.terraform.task.Task;
import net.hollowcube.terraform.task.TaskImpl;
import net.hollowcube.terraform.task.TaskResult;
import net.hollowcube.terraform.util.Format;
import net.hollowcube.terraform.util.ThreadUtil;
import net.minestom.server.instance.ChunkInvalidator;
import net.minestom.server.network.packet.server.play.MultiBlockChangePacket;
import net.minestom.server.utils.validate.Check;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@ApiStatus.Internal
public final class TerraformImpl implements TerraformV2 {
    private static final Logger logger = LoggerFactory.getLogger(TerraformV2.class);

    private final ExecutorService threadPoolCompute;
    private final ExecutorService threadPoolApply;

    TerraformImpl() {
        threadPoolCompute = Executors.newFixedThreadPool(1, new ThreadUtil.NamedThreadFactory("tf-compute"));
        threadPoolApply = Executors.newFixedThreadPool(1, new ThreadUtil.NamedThreadFactory("tf-apply"));
    }


    // Internal Task API

    /**
     * Queues a task to have its compute phase executed, then its apply phase.
     */
    public void submitTask(@NotNull TaskImpl task) {
        logger.debug("{}: submitted", task);
        task.setState(Task.State.QUEUED);

        if (task.computeFunc() != null) {
            threadPoolCompute.submit(computeTask(task));
        } else {
            threadPoolApply.submit(applyTask(task));
        }
    }

    private @NotNull Runnable computeTask(@NotNull TaskImpl task) {
        return () -> {
            try {
                logger.debug("{}: compute started", task);
                task.setState(Task.State.COMPUTE);
                long start = System.nanoTime();

                var world = WorldView.instance(task.session().instance());
                var buffer = task.computeFunc().exec(world);
                task.setBuffer(buffer);

                logger.debug("{}: compute complete in {}ms ({})", task, (System.nanoTime() - start) / 1_000_000d, Format.formatBytes(buffer.sizeBytes()));
                threadPoolApply.submit(applyTask(task));
                task.setState(Task.State.QUEUED);
            } catch (Throwable t) {
                logger.error("{}: compute failed", task, t);
                task.setState(Task.State.FAILED);
            }
        };
    }

    private @NotNull Runnable applyTask(@NotNull TaskImpl task) {
        Check.notNull(task.buffer(), "Task buffer must not be null when applying");
        return () -> {
            try {
                logger.debug("{}: apply started", task);
                task.setState(Task.State.APPLY);
                long start = System.nanoTime();

                var buffer = task.buffer();
                final var changeCount = new AtomicLong();

                var instance = task.session().instance();

                final var sectionChangeCache = new LongArrayList();
                final var paletteData = new int[4096]; // Reused buffer
                final var indexCache = new AtomicInteger(0);

                final var undoBufferBuilder = BlockBuffer.builder();

                buffer.forEachSection((chunkX, chunkY, chunkZ, palette) -> {
                    var chunk = instance.getChunk(chunkX, chunkZ);
                    if (chunk == null) {
                        // Chunk is not loaded, todo should probably notify player
                        logger.warn("{}: reference to unloaded chunk at {}, {}", task, chunkX, chunkZ);
                        chunk = instance.loadChunk(chunkX, chunkZ).join();
//                        return;
                    }

                    sectionChangeCache.clear();

                    var section = chunk.getSection(chunkY);
                    synchronized (chunk) { // Synchronized is OK, we always run this on one of the dedicated threads.
                        //todo optimize palette apply, if the palette is a full chunk of the same block we can do a single fill.
                        //todo replaceall is not working, but that would be ideal. instead we do a get then set.

                        indexCache.set(0);
                        section.blockPalette().getAll((sx, sy, sz, stateId) -> {
                            var paletteIndex = indexCache.getAndIncrement();
                            var newBlockState = palette.get(sx, sy, sz);

                            if (newBlockState == Palette.UNSET) {
                                paletteData[paletteIndex] = stateId;
                            } else {
                                changeCount.incrementAndGet();
                                paletteData[paletteIndex] = newBlockState;
                                sectionChangeCache.add(((long) newBlockState << 12) | ((long) sx << 8 | (long) sz << 4 | sy));

                                // Save old state for undo batch
                                undoBufferBuilder.set(
                                        (chunkX << 4) + sx,
                                        (chunkY << 4) + sy,
                                        (chunkZ << 4) + sz,
                                        stateId
                                );
                            }
                        });

                        if (!task.isDryRun()) {
                            indexCache.set(0);
                            section.blockPalette().setAll((sx, sy, sz) -> paletteData[indexCache.getAndIncrement()]);
                        }
                    }

                    if (!task.isDryRun()) {
                        var updateIndex = (((long) chunkX & 0x3FFFFF) << 42) | ((long) chunkY & 0xFFFFF) | (((long) chunkZ & 0x3FFFFF) << 20);
                        var packet = new MultiBlockChangePacket(updateIndex, sectionChangeCache.toLongArray());
                        chunk.sendPacketToViewers(packet); //todo these could be batched perhaps, maybe minestom does it on its own?
                        ChunkInvalidator.invalidateChunk(chunk);
                    }

                    //todo the client is super laggy when sending many of these, perhaps this should be iterated by vertical chunk and resend the entire chunk if there are enough sections changed
                });

                // Append to history
                var undoBuffer = undoBufferBuilder.build();
                if (!task.isDryRun() && !task.isEphemeral()) {
                    task.session().remember(Change.of(undoBuffer, buffer));
                }

                task.setState(Task.State.COMPLETE);
                logger.debug("{}: apply complete in {}ms", task, (System.nanoTime() - start) / 1_000_000d);

                var postApplyFunc = task.postApplyFunc();
                if (postApplyFunc != null) {
                    var result = new TaskResult(undoBuffer, buffer, changeCount.get());
                    postApplyFunc.exec(result);
                }
            } catch (Throwable t) {
                logger.error("{}: apply failed", task, t);
                task.setState(Task.State.FAILED);
            }
        };
    }

}
