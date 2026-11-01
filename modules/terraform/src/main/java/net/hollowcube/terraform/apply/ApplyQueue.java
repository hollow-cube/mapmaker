package net.hollowcube.terraform.apply;

import net.hollowcube.terraform.apply.ApplyEngine.ApplyContext;
import net.hollowcube.terraform.apply.ApplyEngine.ApplyResult;
import net.hollowcube.terraform.apply.ApplyEngine.ChunkWork;
import net.hollowcube.terraform.buffer.EditBuffer;
import net.minestom.server.instance.Instance;
import net.minestom.server.tag.Tag;
import net.minestom.server.timer.ExecutionType;
import net.minestom.server.timer.TaskSchedule;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;

final class ApplyQueue {
    private static final Tag<ApplyQueue> TAG = Tag.Transient("terraform:apply_queue");

    private final long targetNanos;

    private final Queue<Operation> queue = new ConcurrentLinkedQueue<>();
    private final CompletableFuture<Void> drained = new CompletableFuture<>();
    private volatile boolean closed;

    static ApplyQueue forInstance(Instance instance, long targetNanos) {
        return instance.updateAndGetTag(
            TAG,
            queue -> queue != null ? queue : new ApplyQueue(instance, targetNanos)
        );
    }

    private ApplyQueue(Instance instance, long targetNanos) {
        this.targetNanos = targetNanos;
        instance.scheduler().submitTask(this::run, ExecutionType.TICK_END);
    }

    boolean submit(Operation operation) {
        if (closed) return false;
        queue.add(operation);

        // if we raced a close (and the op hasnt run yet), claw it back
        if (closed && queue.remove(operation)) return false;

        return true;
    }

    /// Rejects further operations. Completes once the queued ones have finished.
    CompletableFuture<Void> close() {
        closed = true;
        return drained;
    }

    /// Runs at the end of every instance tick until the queue is closed and empty.
    private TaskSchedule run() {
        if (!queue.isEmpty()) {
            long deadline = TickDeadline.next(targetNanos);
            for (Operation operation; (operation = queue.peek()) != null;) {
                if (!operation.step(deadline)) break; // Out of time, or still loading
                queue.poll();
                if (System.nanoTime() - deadline >= 0) break;
            }
            TickDeadline.queueFinished();
        }

        if (closed && queue.isEmpty()) {
            drained.complete(null);
            return TaskSchedule.stop();
        }
        return TaskSchedule.nextTick();
    }

    static final class Operation {
        private static final int CHUNK_LOADING = -1;

        private final ApplyEngine engine;
        private final List<ChunkWork> work;
        private final ApplyContext context;
        private final CompletableFuture<ApplyResult> result = new CompletableFuture<>();
        private int next = CHUNK_LOADING;

        /// Starts loading the target chunks. Throws under [UnloadedChunkPolicy#FAIL] if one isn't loaded.
        Operation(
            ApplyEngine engine,
            Instance instance,
            EditBuffer buffer,
            UnloadedChunkPolicy policy,
            boolean captureUndo
        ) {
            this.engine = engine;
            this.work = ApplyEngine.chunkWork(instance, buffer, policy);
            this.context = engine.newContext(instance, captureUndo);
        }

        CompletableFuture<ApplyResult> result() {
            return result;
        }

        /// Writes chunk columns until `deadlineNanos`, at least one. Returns true once [#result] is complete.
        /// Writes nothing while a target chunk is still loading.
        boolean step(long deadlineNanos) {
            try {
                if (next == CHUNK_LOADING) {
                    for (ChunkWork chunkWork : work) {
                        if (!chunkWork.future().isDone()) return false;
                    }
                    ApplyEngine.checkStorage(work);
                    next = 0;
                }

                while (next < work.size()) {
                    // We always apply at least one chunk of the operation to make _some_ progress
                    engine.applyColumn(context, work.get(next++));
                    if (System.nanoTime() - deadlineNanos >= 0) break;
                }
                if (next < work.size()) return false;
                result.complete(context.result());
            } catch (Throwable t) {
                result.completeExceptionally(t);
            }
            return true;
        }
    }
}
